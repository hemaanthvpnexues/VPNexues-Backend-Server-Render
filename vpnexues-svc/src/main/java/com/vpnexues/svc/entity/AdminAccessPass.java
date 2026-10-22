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
 * One issued one-time access pass for a delegated admin (Feature B).
 *
 * <p>Lifecycle: {@code ACTIVE} → consumed on the first successful login ({@code usedAt}) or
 * killed by the Super Admin ({@code invalidated}) or by the clock ({@code expiresAt}).
 * Three wrong passcodes also flip {@code invalidated} — brute-force safe.
 *
 * <p>Only the SHA-256 hex digest of the passcode is stored. The plaintext lives in this
 * transaction's memory for the single generate response and is never written anywhere —
 * same contract as {@code AdminAuditService}: no credential material at rest or in logs.
 * <b>Do not switch to BCrypt here</b>: the login gate compares a candidate digest against
 * {@code passcodeHash}, which needs a deterministic hash.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "admin_access_passes")
public class AdminAccessPass extends BaseEntity {

    /** The delegated admin this pass unlocks. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private AdminUser admin;

    /** Super Admin who issued it — the "who generated it" half of the audit trail. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by")
    private AdminUser generatedBy;

    /** SHA-256 hex (64 chars) of the 6-digit passcode. Never the plaintext. */
    @Column(name = "passcode_hash", nullable = false, length = 64)
    private String passcodeHash;

    /** Deadline — the pass dies here regardless of use. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Wrong codes so far; the pass locks at the configured maximum. */
    @Column(nullable = false)
    private int attempts;

    /** True once used, expired-and-replaced, locked out, or revoked by the Super Admin. */
    @Column(nullable = false)
    private boolean invalidated;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    /** Set on the first successful login — after this the pass can never be replayed. */
    @Column(name = "used_at")
    private Instant usedAt;

    /** Where it was consumed, for the audit trail. */
    @Column(name = "used_ip", length = 45)
    private String usedIp;

    /** Where it was generated. */
    @Column(name = "created_ip", length = 45)
    private String createdIp;
}
