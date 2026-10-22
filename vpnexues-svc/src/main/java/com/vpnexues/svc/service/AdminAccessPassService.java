package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.AdminAccessPassDto;
import com.vpnexues.svc.entity.AdminAccessPass;
import com.vpnexues.svc.entity.AdminRole;
import com.vpnexues.svc.entity.AdminUser;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.repository.AdminAccessPassRepository;
import com.vpnexues.svc.security.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Feature B — one-time access pass (human-friendly 2FA for delegated admin logins).
 *
 * <p>Super Admin issues a 6-digit pass out-of-band (call / WhatsApp / in person); the
 * Sub-Admin types it in after the password. Rules implemented here, exactly as signed off
 * in the proposal:
 *
 * <ul>
 *   <li><b>6 digits, server-generated</b> from {@link SecureRandom} — easy to read aloud.
 *   <li><b>TTL</b> {@code app.admin.access-pass.ttl-minutes} (default 10).
 *   <li><b>Single use</b> — consumed on the first successful login, never replayable.
 *   <li><b>Per-login</b> — every new login needs a fresh pass; re-issuing kills the old one,
 *       so at most one pass per account is ever live.
 *   <li><b>3 attempts</b> — the third wrong code locks the pass (brute-force safe).
 *   <li><b>Hashed at rest</b> — SHA-256 hex, deterministic because the gate compares digests
 *       (see the caution in {@link AdminAccessPass}). The plaintext is never logged.
 *   <li><b>Audited</b> — generation, use, failure, lockout and revocation all land in the
 *       existing {@link AdminAuditService} trail with the client IP, plus the who/for-whom/
 *       when/IP columns kept on the pass row itself.
 *   <li><b>Out-of-band only</b> — no email/SMS provider is touched anywhere in here.
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminAccessPassService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_DIGITS = 6;

    private final AdminAccessPassRepository accessPassRepository;
    private final AdminAuditService auditService;

    /**
     * Whether this account must present a live one-time pass on login (Feature B scope).
     *
     * <p>Two exemptions only:
     *
     * <ul>
     *   <li>{@link AdminRole#SUPER_ADMIN} — never issues a pass to itself;
     *   <li>accounts with {@code access_pass_required = FALSE} — the four pre-existing
     *       country sub-admins (IN / USA / AED / SG) that V51 grandfathered in, plus
     *       anything else created before that migration.
     * </ul>
     *
     * <p>Everyone else — the "5th sub-admin onward" created via Add Sub Admin — is
     * gated, which is the rule signed off with the product owner.
     */
    public boolean requiresPass(AdminUser admin) {
        return admin.getRole() != AdminRole.SUPER_ADMIN && admin.isAccessPassRequired();
    }

    /** Configurable per the proposal ("10 minutes by default, configurable"). */
    @Value("${app.admin.access-pass.ttl-minutes:10}")
    private long ttlMinutes = 10;

    /** Wrong codes allowed before the pass locks itself. */
    @Value("${app.admin.access-pass.max-attempts:3}")
    private int maxAttempts = 3;

    // ───────────────────────────────────────────────────────────── generate ──

    /**
     * Issue a fresh pass for {@code target} and return it — the only moment the plaintext
     * exists outside the Sub-Admin's head. Any pass still in the wild for this account is
     * invalidated first, so a leaked older code dies immediately.
     */
    public AdminAccessPassDto generate(AdminUser target, AdminUser generatedBy, HttpServletRequest request) {
        if (target.getRole() == AdminRole.SUPER_ADMIN) {
            throw new BadRequestException("Access passes are only issued to delegated admin accounts.");
        }
        if (!target.isActive()) {
            throw new BadRequestException("This account is disabled — activate it before issuing an access pass.");
        }

        Instant now = Instant.now();
        for (AdminAccessPass stale : accessPassRepository.findByAdminIdAndUsedAtIsNullAndInvalidatedFalse(target.getId())) {
            stale.setInvalidated(true);
            stale.setInvalidatedAt(now);
            accessPassRepository.save(stale);
        }

        String code = nextCode();
        AdminAccessPass pass = new AdminAccessPass();
        pass.setAdmin(target);
        pass.setGeneratedBy(generatedBy);
        pass.setPasscodeHash(sha256Hex(code));
        pass.setExpiresAt(now.plus(ttlMinutes, ChronoUnit.MINUTES));
        pass.setAttempts(0);
        pass.setInvalidated(false);
        pass.setCreatedIp(ClientIpUtil.from(request));
        accessPassRepository.save(pass);

        audit(target.getEmail(), "PASSCODE_GENERATED", request);
        log.info("Access pass issued: adminId={}, generatedBy={}, expiresAt={}, ip={}",
                target.getId(), generatedBy.getId(), pass.getExpiresAt(), pass.getCreatedIp());
        return toDto(pass, code);
    }

    // ──────────────────────────────────────────────────────────────── login ──

    /**
     * The login gate. Delegated admins only — call this after the password matched and
     * before any JWT/cookie is minted.
     *
     * <p>Consumes the pass on success; counts and audit-logs every failure; locks the pass
     * on the third wrong code. Always throws {@link BadRequestException} with a message that
     * is safe to show a human (no hints about stored state, no credential material).
     */
    public void verifyForLogin(AdminUser admin, String code, HttpServletRequest request) {
        AdminAccessPass pass = accessPassRepository
                .findTopByAdminIdOrderByCreatedAtDesc(admin.getId())
                .orElse(null);

        if (pass == null) {
            audit(admin.getEmail(), "PASSCODE_MISSING", request);
            throw new BadRequestException("Access pass required. Ask your Super Admin to generate one.");
        }
        if (pass.getUsedAt() != null) {
            audit(admin.getEmail(), "PASSCODE_REUSED", request);
            throw new BadRequestException("This access pass was already used. Ask your Super Admin for a new one.");
        }
        if (pass.isInvalidated()) {
            audit(admin.getEmail(), "PASSCODE_INVALIDATED", request);
            throw new BadRequestException("This access pass is no longer valid. Ask your Super Admin for a new one.");
        }
        if (Instant.now().isAfter(pass.getExpiresAt())) {
            pass.setInvalidated(true);
            pass.setInvalidatedAt(Instant.now());
            accessPassRepository.save(pass);
            audit(admin.getEmail(), "PASSCODE_EXPIRED", request);
            throw new BadRequestException("Access pass expired. Ask your Super Admin to generate a new one.");
        }
        if (code == null || code.isBlank()) {
            // A blank field is a UI omission, not an attack — don't burn an attempt for it.
            audit(admin.getEmail(), "PASSCODE_MISSING", request);
            throw new BadRequestException("Enter the one-time access pass from your Super Admin.");
        }

        if (!constantTimeEquals(pass.getPasscodeHash(), sha256Hex(code.trim()))) {
            int attempts = pass.getAttempts() + 1;
            pass.setAttempts(attempts);
            if (attempts >= maxAttempts) {
                pass.setInvalidated(true);
                pass.setInvalidatedAt(Instant.now());
                accessPassRepository.save(pass);
                audit(admin.getEmail(), "PASSCODE_LOCKED", request);
                log.warn("Access pass locked after {} wrong attempts: adminId={}, ip={}",
                        attempts, admin.getId(), ClientIpUtil.from(request));
                throw new BadRequestException(
                        "Too many wrong access-pass attempts — this pass is locked. Ask your Super Admin for a new one.");
            }
            accessPassRepository.save(pass);
            audit(admin.getEmail(), "PASSCODE_INVALID", request);
            throw new BadRequestException("Incorrect access pass.");
        }

        pass.setUsedAt(Instant.now());
        pass.setUsedIp(ClientIpUtil.from(request));
        accessPassRepository.save(pass);
        audit(admin.getEmail(), "PASSCODE_USED", request);
        log.info("Access pass consumed: adminId={}, ip={}", admin.getId(), pass.getUsedIp());
    }

    // ────────────────────────────────────────────────────────── super-admin ──

    /** Live pass for this account (code stripped), or {@code null} when nothing is pending. */
    @Transactional(readOnly = true)
    public AdminAccessPassDto active(UUID adminId) {
        return accessPassRepository
                .findTopByAdminIdOrderByCreatedAtDesc(adminId)
                .map(p -> toDto(p, null))
                .orElse(null);
    }

    /** Newest-first history for the detail screen: who issued, when used, from which IP. */
    @Transactional(readOnly = true)
    public List<AdminAccessPassDto> history(UUID adminId) {
        return accessPassRepository.findTop20ByAdminIdOrderByCreatedAtDesc(adminId).stream()
                .map(p -> toDto(p, null))
                .toList();
    }

    /** Instant revocation of an un-used pass from the same panel. */
    public void invalidate(UUID adminId, HttpServletRequest request) {
        AdminAccessPass pass = accessPassRepository
                .findTopByAdminIdOrderByCreatedAtDesc(adminId)
                .orElseThrow(() -> new BadRequestException("No access pass on this account."));

        if (pass.getUsedAt() != null) {
            throw new BadRequestException("This access pass was already used — nothing to revoke.");
        }
        if (pass.isInvalidated()) {
            throw new BadRequestException("This access pass is already revoked.");
        }
        pass.setInvalidated(true);
        pass.setInvalidatedAt(Instant.now());
        accessPassRepository.save(pass);
        audit(pass.getAdmin().getEmail(), "PASSCODE_INVALIDATED_BY_SUPER_ADMIN", request);
    }

    // ────────────────────────────────────────────────────────────── helpers ──

    private String nextCode() {
        // Bound the range to CODE_DIGITS digits (000000–999999), zero-padded so the
        // number never loses a digit — the Super Admin reads it aloud digit by digit.
        int bound = (int) Math.pow(10, CODE_DIGITS);
        return String.format("%0" + CODE_DIGITS + "d", RANDOM.nextInt(bound));
    }

    /** SHA-256 hex digest — matches the refresh-token convention in {@code JwtService}. */
    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** Length-independent, constant-time comparison — no timing leak on the stored digest. */
    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private String statusOf(AdminAccessPass pass) {
        if (pass.isInvalidated()) return "INVALIDATED";
        if (pass.getUsedAt() != null) return "USED";
        if (Instant.now().isAfter(pass.getExpiresAt())) return "EXPIRED";
        return "ACTIVE";
    }

    private AdminAccessPassDto toDto(AdminAccessPass pass, String code) {
        return new AdminAccessPassDto(
                pass.getId(),
                pass.getAdmin().getId(),
                code,
                pass.getCreatedAt(),
                pass.getExpiresAt(),
                Math.max(0, maxAttempts - pass.getAttempts()),
                statusOf(pass),
                pass.getGeneratedBy() != null ? pass.getGeneratedBy().getName() : null,
                pass.getUsedAt(),
                pass.getUsedIp());
    }

    private void audit(String email, String reason, HttpServletRequest request) {
        auditService.recordSecurityEvent(email, reason, request);
    }
}
