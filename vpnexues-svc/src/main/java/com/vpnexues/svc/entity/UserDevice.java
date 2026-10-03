package com.vpnexues.svc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row per trusted browser/device of a customer (see {@code TrustedDeviceService}).
 *
 * <p>A device earns this row by passing OTP once; from then on it can use the
 * OTP-less {@code POST /api/auth/login} path while its `vpx_dv` cookie matches
 * {@link #deviceTokenHash}. The raw cookie value is never stored — only the
 * SHA-256 hex digest — so a database leak cannot be replayed as a trusted device.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_devices")
public class UserDevice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** SHA-256 hex digest of the opaque `vpx_dv` cookie value. */
    @Column(name = "device_token_hash", nullable = false, length = 64)
    private String deviceTokenHash;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /** Last client IP seen on this device (from X-Forwarded-For when behind the proxy). */
    @Column(length = 64)
    private String ip;

    /** Bumped on every trusted login — used for the least-recently-used device eviction. */
    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    /** Set when the user (or the device cap) revokes this device. */
    @Column(name = "revoked_at")
    private Instant revokedAt;
}
