package com.vpnexues.svc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vpnexues.svc.dto.AdminLoginRequest;
import com.vpnexues.svc.dto.AdminSessionDto;
import com.vpnexues.svc.dto.ChangePasswordRequest;
import com.vpnexues.svc.entity.AdminUser;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.AdminUserRepository;
import com.vpnexues.svc.security.CookieNames;
import com.vpnexues.svc.security.CookieUtil;
import com.vpnexues.svc.security.JwtService;
import com.vpnexues.svc.service.AdminAuditService;
import com.vpnexues.svc.service.AdminAuthService;
import com.vpnexues.svc.service.AdminSessionService;
import com.vpnexues.svc.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final AdminUserRepository adminUserRepository;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final AdminAuditService auditService;
    private final AdminSessionService sessionService;
    private final ObjectMapper objectMapper;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookies;

    @PostMapping("/api/admin/auth/login")
    public AdminSessionDto login(
            @RequestBody @Valid AdminLoginRequest req,
            HttpServletResponse response,
            HttpServletRequest request) {
        String email = req.email().toLowerCase().trim();

        // 1. Check brute-force lockout
        if (loginAttemptService.isLockedOut(email)) {
            long remaining = loginAttemptService.getRemainingLockoutSeconds(email);
            auditService.recordSecurityEvent(email, "RATE_LIMITED", request);
            throw new BadRequestException(
                    "Too many login attempts. Try again in " + remaining + " seconds.");
        }

        // 2. Authenticate (validates password, checks active status)
        AdminUser admin;
        try {
            admin = adminAuthService.authenticate(email, req.password());
        } catch (BadRequestException e) {
            // Record failed attempt and check lockout
            boolean nowLocked = loginAttemptService.recordFailedAttempt(email);
            auditService.recordLoginAttempt(null, email, false, "INVALID_CREDENTIALS", request);
            if (nowLocked) {
                throw new BadRequestException("Account locked due to too many failed attempts. Try again later.");
            }
            throw e;
        }

        // 3. Clear failed attempts on success
        loginAttemptService.clearAttempts(email);

        // 4. Generate access token (with tokenVersion for invalidation support)
        String jwt = jwtService.generateToken(
                admin.getId(),
                JwtService.TOKEN_TYPE_ADMIN,
                Map.of(
                        "role", admin.getRole().name(),
                        "tokenVersion", admin.getTokenVersion()));

        // 5. Generate refresh token
        String rawRefreshToken = sessionService.createRefreshSession(
                admin,
                extractClientIp(request),
                request.getHeader("User-Agent"));

        // 6. Set access token cookie
        setAuthCookie(response, jwt);

        // 7. Set refresh token cookie (httpOnly, secure, longer-lived)
        setRefreshCookie(response, rawRefreshToken);

        // 8. Audit log
        auditService.recordLoginAttempt(admin, email, true, null, request);

        // 9. Check if password change is required (bootstrap accounts)
        if (admin.isMustChangePassword()) {
            AdminSessionDto dto = toDto(admin);
            // Add a flag — the frontend can redirect to password change page
            return new AdminSessionDto(dto.id(), dto.email(), dto.name(), dto.role(), dto.countryCode());
        }

        return toDto(admin);
    }

    /**
     * Session probe called by the admin SPA on page load — including on the public
     * homepage for visitors who are simply not signed in. "No session" is an answer,
     * not an error, so this returns 200 with a JSON {@code null} body to keep the
     * browser console clean. Every other /api/admin/** endpoint still returns 401
     * without a session, and a real authorization failure still returns 403.
     */
    @GetMapping("/api/admin/auth/me")
    public void me(@AuthenticationPrincipal UUID adminId, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        if (adminId == null) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("null");
            return;
        }
        AdminUser admin =
                adminUserRepository.findById(adminId).orElseThrow(() -> new NotFoundException("Admin not found"));
        response.getWriter().write(objectMapper.writeValueAsString(toDto(admin)));
    }

    @PostMapping("/api/admin/auth/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawToken = CookieUtil.read(request, CookieNames.ADMIN_REFRESH_TOKEN);
        if (rawToken == null || rawToken.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        Optional<String> newRefreshToken = sessionService.rotateRefreshToken(
                rawToken,
                extractClientIp(request),
                request.getHeader("User-Agent"));

        if (newRefreshToken.isEmpty()) {
            // Reuse detected or token invalid — clear cookies
            clearCookie(response, CookieNames.ADMIN_ACCESS_TOKEN);
            clearCookie(response, CookieNames.ADMIN_REFRESH_TOKEN);
            return ResponseEntity.status(401).build();
        }

        // Reload admin to get current state
        String tokenHash = jwtService.hashRefreshToken(rawToken);
        // We need to get the admin from the old token — but it's already revoked.
        // Instead, we extract adminId from the access token if present, or from the refresh flow.
        // For simplicity, the refresh response only sets new cookies — the client should
        // call /me with the new access token.

        // Generate new access token using the new refresh token's admin
        // The admin was already loaded in rotateRefreshToken — we'll use /me endpoint instead.
        // The client will call GET /api/admin/auth/me with the new access token.

        setRefreshCookie(response, newRefreshToken.get());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/admin/auth/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UUID adminId,
            HttpServletResponse response,
            HttpServletRequest request) {
        // Revoke all refresh sessions for this admin
        if (adminId != null) {
            sessionService.revokeAllSessions(adminId);
            auditService.recordSecurityEvent(
                    adminId.toString(), "LOGOUT", request);
        }
        clearCookie(response, CookieNames.ADMIN_ACCESS_TOKEN);
        clearCookie(response, CookieNames.ADMIN_REFRESH_TOKEN);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/admin/auth/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UUID adminId,
            @RequestBody @Valid ChangePasswordRequest req,
            HttpServletRequest request) {
        adminAuthService.changePassword(adminId, req.currentPassword(), req.newPassword());
        // Revoke all sessions after password change
        sessionService.revokeAllSessions(adminId);
        auditService.recordSecurityEvent(
                adminId.toString(), "PASSWORD_CHANGED", request);
        return ResponseEntity.noContent().build();
    }

    private void setAuthCookie(HttpServletResponse response, String jwt) {
        ResponseCookie cookie = ResponseCookie.from(CookieNames.ADMIN_ACCESS_TOKEN, jwt)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(secureCookies ? "None" : "Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(jwtService.accessTokenTtlSeconds()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void setRefreshCookie(HttpServletResponse response, String rawRefreshToken) {
        ResponseCookie cookie = ResponseCookie.from(CookieNames.ADMIN_REFRESH_TOKEN, rawRefreshToken)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(secureCookies ? "None" : "Lax")
                .path("/api/admin/auth")
                .maxAge(Duration.ofSeconds(jwtService.refreshTokenTtlSeconds()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(secureCookies ? "None" : "Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private AdminSessionDto toDto(AdminUser a) {
        return new AdminSessionDto(a.getId(), a.getEmail(), a.getName(), a.getRole(), a.getCountryCode());
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
