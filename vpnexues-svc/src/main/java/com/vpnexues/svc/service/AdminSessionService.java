package com.vpnexues.svc.service;

import com.vpnexues.svc.entity.AdminRefreshToken;
import com.vpnexues.svc.entity.AdminUser;
import com.vpnexues.svc.repository.AdminRefreshTokenRepository;
import com.vpnexues.svc.repository.AdminUserRepository;
import com.vpnexues.svc.security.JwtService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages admin refresh tokens (server-controlled sessions).
 * Handles: creation, rotation, reuse detection, revocation, cleanup.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSessionService {

    private final AdminRefreshTokenRepository refreshTokenRepository;
    private final AdminUserRepository adminUserRepository;
    private final JwtService jwtService;

    /**
     * Create a new refresh token session for an admin.
     * Returns the raw token (to be sent to the client).
     */
    @Transactional
    public String createRefreshSession(
            AdminUser admin,
            String ipAddress,
            String userAgent) {
        String rawToken = jwtService.generateRefreshToken();
        String tokenHash = jwtService.hashRefreshToken(rawToken);

        AdminRefreshToken session = new AdminRefreshToken();
        session.setTokenHash(tokenHash);
        session.setAdmin(admin);
        session.setSessionId(UUID.randomUUID());
        session.setExpiresAt(Instant.now().plusSeconds(jwtService.refreshTokenTtlSeconds()));
        session.setRevoked(false);
        session.setCreatedIp(ipAddress);
        session.setUserAgent(userAgent);
        refreshTokenRepository.save(session);

        return rawToken;
    }

    /** New raw refresh token plus the admin it belongs to — the controller needs both to mint a fresh access cookie. */
    public record RefreshResult(String rawRefreshToken, AdminUser admin) {}

    /**
     * Rotate a refresh token: revoke old, issue new.
     * Returns the new raw refresh token with its admin, or empty if invalid/expired/revoked.
     */
    @Transactional
    public Optional<RefreshResult> rotateRefreshToken(
            String rawToken,
            String ipAddress,
            String userAgent) {
        String tokenHash = jwtService.hashRefreshToken(rawToken);
        Optional<AdminRefreshToken> existingOpt = refreshTokenRepository.findByTokenHash(tokenHash);

        if (existingOpt.isEmpty()) {
            log.warn("Refresh token not found: possible theft or already rotated");
            return Optional.empty();
        }

        AdminRefreshToken existing = existingOpt.get();

        // Check if revoked (reuse detection)
        if (existing.isRevoked()) {
            log.warn("REFRESH_TOKEN_REUSE_DETECTED: sessionId={}, adminId={}. "
                    + "Revoking all sessions for this admin.",
                    existing.getSessionId(), existing.getAdmin().getId());
            // Revoke ALL sessions for this admin (theft response)
            refreshTokenRepository.revokeAllByAdminId(existing.getAdmin().getId());
            return Optional.empty();
        }

        // Check expiry
        if (existing.getExpiresAt().isBefore(Instant.now())) {
            log.info("Refresh token expired: sessionId={}", existing.getSessionId());
            return Optional.empty();
        }

        // Reload admin from database to get current state (role, country, enabled, tokenVersion)
        Optional<AdminUser> adminOpt = adminUserRepository.findById(existing.getAdmin().getId());
        if (adminOpt.isEmpty() || !adminOpt.get().isActive()) {
            log.warn("Admin not found or disabled during refresh: adminId={}", existing.getAdmin().getId());
            existing.setRevoked(true);
            existing.setRevokedAt(Instant.now());
            refreshTokenRepository.save(existing);
            return Optional.empty();
        }

        AdminUser admin = adminOpt.get();

        // Revoke the old token
        existing.setRevoked(true);
        existing.setRevokedAt(Instant.now());

        // Issue new token
        String newRawToken = jwtService.generateRefreshToken();
        String newHash = jwtService.hashRefreshToken(newRawToken);
        existing.setReplacedById(null); // Will be set after save

        AdminRefreshToken newSession = new AdminRefreshToken();
        newSession.setTokenHash(newHash);
        newSession.setAdmin(admin);
        newSession.setSessionId(existing.getSessionId()); // Keep same session
        newSession.setExpiresAt(Instant.now().plusSeconds(jwtService.refreshTokenTtlSeconds()));
        newSession.setRevoked(false);
        newSession.setCreatedIp(ipAddress);
        newSession.setUserAgent(userAgent);

        refreshTokenRepository.save(existing);
        refreshTokenRepository.save(newSession);

        return Optional.of(new RefreshResult(newRawToken, admin));
    }

    /**
     * Revoke all refresh sessions for an admin (used on password change, account disable, etc.).
     */
    @Transactional
    public void revokeAllSessions(UUID adminId) {
        int revoked = refreshTokenRepository.revokeAllByAdminId(adminId);
        log.info("Revoked {} refresh session(s) for adminId={}", revoked, adminId);
    }

    /**
     * Delete expired refresh tokens (can be called by a scheduled task).
     */
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpired(Instant.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired admin refresh tokens", deleted);
        }
    }
}
