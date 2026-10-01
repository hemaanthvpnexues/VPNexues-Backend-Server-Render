package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.OtpSendRequest;
import com.vpnexues.svc.dto.OtpVerifyRequest;
import com.vpnexues.svc.dto.PhoneLoginRequest;
import com.vpnexues.svc.dto.UserDto;
import com.vpnexues.svc.entity.User;
import com.vpnexues.svc.security.CookieNames;
import com.vpnexues.svc.security.CookieUtil;
import com.vpnexues.svc.security.JwtService;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.UserRepository;
import com.vpnexues.svc.service.CartService;
import com.vpnexues.svc.service.CustomerAuthService;
import com.vpnexues.svc.service.PhoneLoginRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
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
public class AuthController {

    private final CustomerAuthService customerAuthService;
    private final CartService cartService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PhoneLoginRateLimiter phoneLoginRateLimiter;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookies;

    /**
     * Sends OTP via backend (stub provider). Firebase Auth on the client handles
     * phone OTP directly — this endpoint is kept for legacy/fallback use.
     */
    @PostMapping("/api/auth/otp/send")
    public ResponseEntity<Void> sendOtp(@RequestBody @Valid OtpSendRequest req) {
        customerAuthService.sendOtp(req.phone());
        return ResponseEntity.noContent().build();
    }

    /**
     * Verifies OTP. Supports two modes:
     * 1. Firebase mode: client sends firebaseUid + accessToken (Firebase ID token) — backend
     *    verifies the token and finds/creates user by Firebase UID
     * 2. Legacy mode: client sends phone + code — backend verifies via stub OTP provider
     */
    @PostMapping("/api/auth/otp/verify")
    public UserDto verifyOtp(
            @RequestBody @Valid OtpVerifyRequest req, HttpServletRequest request, HttpServletResponse response) {

        User user;

        if (req.firebaseUid() != null && req.accessToken() != null) {
            // Server-side check: Firebase ID token must be valid for this project
            var claims = jwtService.parseFirebaseToken(req.accessToken());
            if (claims == null || !req.firebaseUid().equals(claims.getSubject())) {
                throw new com.vpnexues.svc.exception.BadRequestException("Invalid Firebase session");
            }
            user = customerAuthService.verifyWithFirebase(req.firebaseUid(), req.phone(), req.name(), req.email());
        } else {
            // Legacy mode: verify OTP via backend provider
            user = customerAuthService.verifyOtpAndResolveUser(req.phone(), req.code(), req.name(), req.email());
        }

        return issueSession(user, request, response);
    }

    /**
     * OTP-less login: the phone number alone resolves a known account and starts a session —
     * no SMS/Firebase OTP is sent, so returning users never burn OTP quota.
     *
     * <p>Unknown numbers get 404 so the client can route them to signup instead.
     * Because there is no secret, PhoneLoginRateLimiter caps probing attempts
     * (5/number, 30/IP per 5 minutes → HTTP 429).
     */
    @PostMapping("/api/auth/login")
    public UserDto loginByPhone(
            @RequestBody @Valid PhoneLoginRequest req, HttpServletRequest request, HttpServletResponse response) {

        phoneLoginRateLimiter.check(req.phone(), request);

        User user = userRepository.findByPhone(req.phone())
                .orElseThrow(() -> new NotFoundException("No account found for this mobile number"));

        return issueSession(user, request, response);
    }

    /**
     * Merges any guest cart, issues the 30-day customer JWT and sets the `vpx_at` cookie.
     * Shared by OTP verify and OTP-less login so both paths behave identically.
     */
    private UserDto issueSession(User user, HttpServletRequest request, HttpServletResponse response) {
        // Merge guest cart into user cart. The vpx_guest cookie is deliberately NOT cleared here:
        // a "Add to cart" that was still in flight while this login ran can land in the guest cart
        // after this merge and would then be stranded forever. CartOwnerResolver folds any leftover
        // guest cart into the user cart on the next authenticated cart call and clears the cookie
        // there, so late-arriving items are still recovered.
        String guestToken = CookieUtil.read(request, CookieNames.GUEST_CART_TOKEN);
        cartService.mergeGuestCartIntoUser(guestToken, user.getId());

        // Issue backend JWT (long-lived customer session - see JwtService#generateCustomerToken)
        String jwt = jwtService.generateCustomerToken(user.getId());
        setAuthCookie(response, jwt);

        return UserDto.of(user);
    }

    @GetMapping("/api/auth/me")
    public UserDto me(@AuthenticationPrincipal UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        return UserDto.of(user);
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        clearCookie(response, CookieNames.CUSTOMER_ACCESS_TOKEN);
        return ResponseEntity.noContent().build();
    }

    private void setAuthCookie(HttpServletResponse response, String jwt) {
        ResponseCookie cookie = ResponseCookie.from(CookieNames.CUSTOMER_ACCESS_TOKEN, jwt)
                .httpOnly(true)
                .secure(secureCookies)
                // Cross-site (site on GoDaddy → API on Render) requires None+Secure;
                // local HTTP keeps Lax.
                .sameSite(secureCookies ? "None" : "Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(jwtService.customerAccessTokenTtlSeconds()))
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
}
