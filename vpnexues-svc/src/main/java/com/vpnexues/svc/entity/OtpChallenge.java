package com.vpnexues.svc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent OTP challenge — one row per (channel, identifier), shared by every {@code OtpProvider}
 * and by email verification. See {@code OtpChallengeService} for the generate/verify logic. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "otp_challenges")
public class OtpChallenge extends BaseEntity {

    /** "PHONE" or "EMAIL" — kept as a plain string rather than an enum so new channels don't need a migration. */
    @Column(nullable = false, length = 20)
    private String channel;

    @Column(nullable = false)
    private String identifier;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "attempts_remaining", nullable = false)
    private int attemptsRemaining;
}
