package com.vpnexues.svc.service;

import com.vpnexues.svc.entity.User;
import com.vpnexues.svc.entity.UserDevice;
import com.vpnexues.svc.repository.UserDeviceRepository;
import com.vpnexues.svc.security.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trusted-device binding for customer phone login.
 *
 * <p>Two cookie types cooperate:
 * <ul>
 *   <li><b>{@code vpx_at}</b> — the 30-day session JWT (short-lived credential).</li>
 *   <li><b>{@code vpx_dv}</b> — this service's long-lived proof that THIS browser
 *       already passed OTP once. {@code POST /api/auth/login} only issues a session
 *       when this cookie matches an active {@link UserDevice} row; every other
 *       device is challenged with OTP (Firebase) first.</li>
 * </ul>
 *
 * <p>The raw {@code vpx_dv} value exists only in the client's httpOnly cookie;
 * the database stores its SHA-256 hash. Tokens rotate on every trusted login
 * (stolen-cookie window shrinks) and a per-user cap evicts the least-recently
 * seen device so the table cannot grow without bound.
 */
@Service
@RequiredArgsConstructor
public class TrustedDeviceService {

    /** Active trusted devices kept per user; the oldest (least recently seen) is revoked beyond this. */
    static final int MAX_ACTIVE_DEVICES = 5;

    static final int TOKEN_BYTES = 32;

    private final UserDeviceRepository userDeviceRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    /** Rolling lifetime of one trusted device (cookie TTL and server-side check use the same value). */
    @Value("${app.trusted-device.ttl-days:180}")
    private long ttlDays;

    /**
     * When {@code rawToken} is this user's active, unexpired trusted-device token,
     * rotates it (new secret, fresh TTL, latest IP/UA) and returns the new raw token
     * for the refreshed `vpx_dv` cookie. Empty when there is no valid token —
     * the caller must then challenge the device with OTP instead of issuing a session.
     */
    @Transactional
    public Optional<String> renewIfTrusted(User user, String rawToken, HttpServletRequest request) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return userDeviceRepository
                .findByUserAndDeviceTokenHashAndRevokedAtIsNull(user, sha256(rawToken))
                .filter(device -> !isExpired(device))
                .map(device -> {
                    String rotated = newToken();
                    device.setDeviceTokenHash(sha256(rotated));
                    device.setIp(clientIp(request));
                    device.setUserAgent(truncate(ua(request), 512));
                    device.setLastSeenAt(Instant.now());
                    userDeviceRepository.save(device);
                    return rotated;
                });
    }

    /**
     * Records this device as trusted after a successful OTP verification and
     * returns the raw token for the new `vpx_dv` cookie. Enforces the per-user
     * device cap by revoking the least-recently seen overflow device.
     */
    @Transactional
    public String trust(User user, HttpServletRequest request) {
        String raw = newToken();
        UserDevice device = new UserDevice();
        device.setUser(user);
        device.setDeviceTokenHash(sha256(raw));
        device.setIp(clientIp(request));
        device.setUserAgent(truncate(ua(request), 512));
        device.setLastSeenAt(Instant.now());
        userDeviceRepository.save(device);
        enforceDeviceCap(user);
        return raw;
    }

    private void enforceDeviceCap(User user) {
        long active = userDeviceRepository.countByUserAndRevokedAtIsNull(user);
        while (active > MAX_ACTIVE_DEVICES) {
            Optional<UserDevice> oldest =
                    userDeviceRepository.findFirstByUserAndRevokedAtIsNullOrderByLastSeenAtAsc(user);
            if (oldest.isEmpty()) {
                return;
            }
            oldest.get().setRevokedAt(Instant.now());
            userDeviceRepository.save(oldest.get());
            active--;
        }
    }

    private boolean isExpired(UserDevice device) {
        Instant created = device.getCreatedAt();
        if (created == null) {
            return true;
        }
        return created.plus(Duration.ofDays(ttlDays)).isBefore(Instant.now());
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String clientIp(HttpServletRequest request) {
        return truncate(ClientIpUtil.from(request), 64);
    }

    private String ua(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
