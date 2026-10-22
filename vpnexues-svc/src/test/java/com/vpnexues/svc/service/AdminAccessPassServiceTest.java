package com.vpnexues.svc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vpnexues.svc.dto.AdminAccessPassDto;
import com.vpnexues.svc.entity.AdminAccessPass;
import com.vpnexues.svc.entity.AdminRole;
import com.vpnexues.svc.entity.AdminUser;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.repository.AdminAccessPassRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * One-time access pass rules (Feature B — Admin Dashboard proposal, §"Access-pass rules").
 *
 * Rules under test:
 *  - 6-digit server-generated code; only its SHA-256 digest is ever persisted.
 *  - 10-minute TTL; expired pass is dead and self-invalidates on sight.
 *  - Single use: consumed on the first good login, never replayable.
 *  - Third wrong code locks the pass (brute-force safe); a blank field is not an attempt.
 *  - Only one live pass per account: re-issuing kills the older one.
 *  - Delegated accounts only — the Super Admin is never gated.
 *  - Scope after V51: only accounts created after it (the "5th sub-admin onward")
 *    are gated; the four pre-existing country sub-admins sign in with password alone.
 */
class AdminAccessPassServiceTest {

    private AdminAccessPassRepository accessPassRepository;
    private AdminAuditService auditService;
    private AdminAccessPassService service;

    private MockHttpServletRequest request;
    private AdminUser subAdmin;
    private AdminUser superAdmin;

    @BeforeEach
    void setUp() {
        accessPassRepository = mock(AdminAccessPassRepository.class);
        auditService = mock(AdminAuditService.class);
        // ttlMinutes=10 / maxAttempts=3 come from the field initialisers when Spring is absent.
        service = new AdminAccessPassService(accessPassRepository, auditService);

        request = new MockHttpServletRequest();

        subAdmin = admin(AdminRole.SUB_ADMIN, true);
        superAdmin = admin(AdminRole.SUPER_ADMIN, true);

        // Every save hands the same entity back, so tests can assert on what was written.
        when(accessPassRepository.save(any(AdminAccessPass.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- generate ----------

    @Test
    void generateReturnsSixDigitCodeAndStoresOnlyItsHash() {
        when(accessPassRepository.findByAdminIdAndUsedAtIsNullAndInvalidatedFalse(subAdmin.getId()))
                .thenReturn(List.of());

        AdminAccessPassDto dto = service.generate(subAdmin, superAdmin, request);

        assertNotNull(dto.code());
        assertTrue(dto.code().matches("\\d{6}"), "expected 6 digits, got: " + dto.code());
        assertEquals("ACTIVE", dto.status());
        assertNotNull(dto.expiresAt());
        assertTrue(dto.expiresAt().isAfter(Instant.now().plus(9, ChronoUnit.MINUTES)));
        assertTrue(dto.expiresAt().isBefore(Instant.now().plus(11, ChronoUnit.MINUTES)));

        // The plaintext must never be what lands in the table.
        AdminAccessPass saved = captureSaved();
        assertNotEquals(dto.code(), saved.getPasscodeHash());
        assertEquals(64, saved.getPasscodeHash().length());
        verify(auditService).recordSecurityEvent(eq(subAdmin.getEmail()), eq("PASSCODE_GENERATED"), any());
    }

    @Test
    void generateInvalidatesAnyPassStillInTheWild() {
        AdminAccessPass stale = livePass(subAdmin);
        when(accessPassRepository.findByAdminIdAndUsedAtIsNullAndInvalidatedFalse(subAdmin.getId()))
                .thenReturn(List.of(stale));

        service.generate(subAdmin, superAdmin, request);

        assertTrue(stale.isInvalidated(), "older live pass must die when a new one is issued");
        assertNotNull(stale.getInvalidatedAt());
    }

    @Test
    void generateRefusesSuperAdminAndDisabledAccounts() {
        assertThrows(BadRequestException.class, () -> service.generate(superAdmin, superAdmin, request));

        AdminUser disabled = admin(AdminRole.SUB_ADMIN, false);
        assertThrows(BadRequestException.class, () -> service.generate(disabled, superAdmin, request));
    }

    // ---------- gate scope (V51: only the "5th sub-admin onward") ----------

    @Test
    void superAdminIsNeverGated() {
        assertFalse(service.requiresPass(superAdmin), "SUPER_ADMIN signs in with the password alone");
    }

    @Test
    void grandfatheredCountrySubAdminsSkipThePass() {
        // The four pre-existing IN/USA/AED/SG accounts, backfilled by V51.
        subAdmin.setAccessPassRequired(false);

        assertFalse(
                service.requiresPass(subAdmin),
                "V51-exempt sub-admin must log in with password alone, no pass");
    }

    @Test
    void subAdminCreatedAfterV51IsGated() {
        // New AdminUser() — the state AdminUserManagementService.create() produces.
        assertTrue(service.requiresPass(subAdmin), "the 5th sub-admin onward must present a pass");
    }

    // ---------- successful login ----------

    @Test
    void verifyConsumesThePassOnFirstSuccess() {
        AdminAccessPass pass = livePass(subAdmin);
        stubLatest(pass);
        String code = "472916";

        // Store the digest of the code the Super Admin "read aloud".
        pass.setPasscodeHash(sha256(code));

        service.verifyForLogin(subAdmin, code, request);

        assertNotNull(pass.getUsedAt(), "pass must be consumed on success");
        assertEquals("127.0.0.1", pass.getUsedIp());
        verify(auditService).recordSecurityEvent(eq(subAdmin.getEmail()), eq("PASSCODE_USED"), any());
    }

    @Test
    void verifyRejectsASecondUseOfTheSamePass() {
        AdminAccessPass pass = livePass(subAdmin);
        pass.setUsedAt(Instant.now());
        stubLatest(pass);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.verifyForLogin(subAdmin, "472916", request));
        assertTrue(ex.getMessage().contains("already used"));
    }

    // ---------- failures, attempts, lockout ----------

    @Test
    void wrongCodeBurnsOneAttemptAndSaysSo() {
        AdminAccessPass pass = livePass(subAdmin);
        pass.setPasscodeHash(sha256("472916"));
        stubLatest(pass);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.verifyForLogin(subAdmin, "000000", request));

        assertEquals("Incorrect access pass.", ex.getMessage());
        assertEquals(1, pass.getAttempts());
        assertFalse(pass.isInvalidated());
        verify(auditService).recordSecurityEvent(eq(subAdmin.getEmail()), eq("PASSCODE_INVALID"), any());
    }

    @Test
    void thirdWrongCodeLocksThePassPermanently() {
        AdminAccessPass pass = livePass(subAdmin);
        pass.setPasscodeHash(sha256("472916"));
        pass.setAttempts(2); // two strikes already
        stubLatest(pass);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.verifyForLogin(subAdmin, "000000", request));

        assertTrue(ex.getMessage().contains("locked"), "message must warn the human");
        assertTrue(pass.isInvalidated(), "third wrong code kills the pass");
        assertNotNull(pass.getInvalidatedAt());
        verify(auditService).recordSecurityEvent(eq(subAdmin.getEmail()), eq("PASSCODE_LOCKED"), any());

        // ...and a later login with the right code is still refused — the pass is dead.
        stubLatest(pass);
        assertThrows(BadRequestException.class, () -> service.verifyForLogin(subAdmin, "472916", request));
    }

    @Test
    void blankCodeIsRejectedWithoutBurningAnAttempt() {
        AdminAccessPass pass = livePass(subAdmin);
        pass.setPasscodeHash(sha256("472916"));
        stubLatest(pass);

        assertThrows(BadRequestException.class, () -> service.verifyForLogin(subAdmin, "  ", request));

        assertEquals(0, pass.getAttempts(), "a missing field is a UI omission, not a strike");
        assertFalse(pass.isInvalidated());
    }

    // ---------- missing / expired / invalidated ----------

    @Test
    void loginWithoutAnyPassIsRefused() {
        stubLatest(null);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.verifyForLogin(subAdmin, "472916", request));
        assertTrue(ex.getMessage().contains("Generate one") || ex.getMessage().contains("generate one"));
        verify(auditService).recordSecurityEvent(eq(subAdmin.getEmail()), eq("PASSCODE_MISSING"), any());
    }

    @Test
    void expiredPassIsDeadAndSelfInvalidates() {
        AdminAccessPass pass = livePass(subAdmin);
        pass.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        pass.setPasscodeHash(sha256("472916"));
        stubLatest(pass);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.verifyForLogin(subAdmin, "472916", request));

        assertTrue(ex.getMessage().contains("expired"));
        assertTrue(pass.isInvalidated(), "seeing an expired pass must kill it");
        verify(auditService).recordSecurityEvent(eq(subAdmin.getEmail()), eq("PASSCODE_EXPIRED"), any());
    }

    @Test
    void revokedPassIsRefused() {
        AdminAccessPass pass = livePass(subAdmin);
        pass.setInvalidated(true);
        stubLatest(pass);

        assertThrows(BadRequestException.class, () -> service.verifyForLogin(subAdmin, "472916", request));
        verify(auditService).recordSecurityEvent(eq(subAdmin.getEmail()), eq("PASSCODE_INVALIDATED"), any());
    }

    // ---------- super-admin panel ----------

    @Test
    void activeStripsTheCodeSoItIsNeverServedTwice() {
        AdminAccessPass pass = livePass(subAdmin);
        stubLatest(pass);

        AdminAccessPassDto dto = service.active(subAdmin.getId());

        assertNotNull(dto);
        assertNull(dto.code(), "status endpoint must not hand the plaintext back");
        assertEquals("ACTIVE", dto.status());
        assertEquals(3, dto.attemptsRemaining());
    }

    @Test
    void activeReportsStatusesAndAttemptsLeft() {
        AdminAccessPass used = livePass(subAdmin);
        used.setUsedAt(Instant.now());
        stubLatest(used);
        assertEquals("USED", service.active(subAdmin.getId()).status());

        AdminAccessPass exhausted = livePass(subAdmin);
        exhausted.setAttempts(3);
        stubLatest(exhausted);
        AdminAccessPassDto dto = service.active(subAdmin.getId());
        assertEquals("ACTIVE", dto.status());
        assertEquals(0, dto.attemptsRemaining());
    }

    @Test
    void activeIsNullWhenNothingIsPending() {
        stubLatest(null);
        assertNull(service.active(subAdmin.getId()));
    }

    @Test
    void revokeKillsAnUnusedPassAndAuditsIt() {
        AdminAccessPass pass = livePass(subAdmin);
        stubLatest(pass);

        service.invalidate(subAdmin.getId(), request);

        assertTrue(pass.isInvalidated());
        verify(auditService).recordSecurityEvent(
                eq(subAdmin.getEmail()), eq("PASSCODE_INVALIDATED_BY_SUPER_ADMIN"), any());
    }

    @Test
    void revokeOfAnAlreadyUsedPassIsAManagedError() {
        AdminAccessPass pass = livePass(subAdmin);
        pass.setUsedAt(Instant.now());
        stubLatest(pass);

        assertThrows(BadRequestException.class, () -> service.invalidate(subAdmin.getId(), request));
        verify(auditService, never()).recordSecurityEvent(contains("INVALIDATED_BY_SUPER_ADMIN"), any(), any());
    }

    // ---------- helpers ----------

    private void stubLatest(AdminAccessPass pass) {
        when(accessPassRepository.findTopByAdminIdOrderByCreatedAtDesc(subAdmin.getId()))
                .thenReturn(Optional.ofNullable(pass));
    }

    /** A brand-new, unexpired, unused pass for {@link #subAdmin}. */
    private AdminAccessPass livePass(AdminUser target) {
        AdminAccessPass pass = new AdminAccessPass();
        pass.setAdmin(target);
        pass.setGeneratedBy(superAdmin);
        pass.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        pass.setAttempts(0);
        pass.setInvalidated(false);
        pass.setPasscodeHash(sha256("472916"));
        return pass;
    }

    private AdminUser admin(AdminRole role, boolean active) {
        AdminUser u = new AdminUser();
        u.setId(UUID.randomUUID());
        u.setEmail(role.name().toLowerCase() + "@vpnexues.test");
        u.setName(role.name());
        u.setRole(role);
        u.setActive(active);
        return u;
    }

    private AdminAccessPass captureSaved() {
        org.mockito.ArgumentCaptor<AdminAccessPass> captor =
                org.mockito.ArgumentCaptor.forClass(AdminAccessPass.class);
        verify(accessPassRepository).save(captor.capture());
        return captor.getValue();
    }

    /** Same SHA-256 hex digest the service stores — mirrors JwtService.hashRefreshToken. */
    private String sha256(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
