package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.AdminUserSummaryDto;
import com.vpnexues.svc.dto.CreateAdminUserRequest;
import com.vpnexues.svc.dto.UpdateAdminUserRequest;
import com.vpnexues.svc.entity.AdminRole;
import com.vpnexues.svc.entity.AdminUser;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.AdminLoginAuditRepository;
import com.vpnexues.svc.repository.AdminUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** SUPER_ADMIN-only management of other admin accounts — gated at the controller with @PreAuthorize. */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserManagementService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSessionService sessionService;
    private final AdminLoginAuditRepository adminLoginAuditRepository;
    private final AdminAuditService adminAuditService;

    /** Valid country codes for SUB_ADMIN roles. */
    private static final Set<String> VALID_COUNTRY_CODES = Set.of("IN", "AE", "US", "SG", "LK");

    /** Module/permission keys the super admin may grant — anything else in the request is dropped. */
    private static final Set<String> VALID_MODULES =
            Set.of("dashboard", "orders", "products", "users", "reports", "payments", "support", "content", "careers");
    private static final Set<String> VALID_PERMISSIONS = Set.of("view", "edit", "delete", "export", "hide");

    @Transactional(readOnly = true)
    public List<AdminUserSummaryDto> list() {
        List<AdminUser> admins = adminUserRepository.findAll();
        if (admins.isEmpty()) {
            return List.of();
        }

        // Single grouped query for every admin's last login — the per-row query
        // this replaces was an N+1 and dominated Sub Admins page load time.
        Map<UUID, Instant> lastLoginByAdmin = adminLoginAuditRepository
                .findLatestSuccessfulLogins(admins.stream().map(AdminUser::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        AdminLoginAuditRepository.LatestLoginProjection::getAdminId,
                        AdminLoginAuditRepository.LatestLoginProjection::getLatestAt,
                        (a, b) -> a));

        return admins.stream().map(a -> toDto(a, lastLoginByAdmin.get(a.getId()))).toList();
    }

    public AdminUserSummaryDto create(CreateAdminUserRequest req) {
        // Normalised once and reused for both the duplicate check and the stored
        // value: login looks addresses up lower-cased, so a row saved with any
        // capital letter would be unreachable from then on (see V52).
        String email = normalizeEmail(req.email());
        if (adminUserRepository.findByEmail(email).isPresent()) {
            throw new BadRequestException("An admin account with email '" + email + "' already exists");
        }

        // Prevent creation of additional SUPER_ADMIN accounts through the API.
        // The first SUPER_ADMIN is created via AdminBootstrapRunner.
        if (req.role() == AdminRole.SUPER_ADMIN) {
            throw new BadRequestException("Cannot create SUPER_ADMIN accounts through this endpoint. "
                    + "The initial SUPER_ADMIN is created via bootstrap.");
        }

        // SUB_ADMIN must have a valid country code
        if (req.role() == AdminRole.SUB_ADMIN) {
            if (req.countryCode() == null || req.countryCode().isBlank()
                    || !VALID_COUNTRY_CODES.contains(req.countryCode().toUpperCase())) {
                throw new BadRequestException(
                        "SUB_ADMIN requires a valid country code. Allowed: IN, AE, US, SG, LK");
            }
        }

        AdminUser admin = new AdminUser();
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(req.password()));
        admin.setName(req.name());
        admin.setRole(req.role());
        // Feature B: accounts added from here on are the "5th sub-admin onward", so
        // they need the Super Admin's one-time pass at every login (V51 exempted the
        // four pre-existing country sub-admins — editing those never flips this back).
        admin.setAccessPassRequired(true);
        admin.setPhone(blankToNull(req.phone()));
        admin.setModules(sanitize(req.modules(), VALID_MODULES));
        admin.setPermissions(sanitize(req.permissions(), VALID_PERMISSIONS));
        if (req.countryCode() != null && !req.countryCode().isBlank()) {
            admin.setCountryCode(req.countryCode().toUpperCase());
        }
        return toDto(adminUserRepository.save(admin));
    }

    /**
     * Admin addresses are looked up lower-cased at login, so they must be stored that way too —
     * otherwise the row can never be found again. Trimming keeps the duplicate check honest
     * against a stray trailing space.
     */
    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    /** requestingAdminId prevents a SUPER_ADMIN from deactivating/demoting their own account by accident. */
    public AdminUserSummaryDto update(UUID id, UpdateAdminUserRequest req, UUID requestingAdminId) {
        if (id.equals(requestingAdminId) && (!req.active() || req.role() != AdminRole.SUPER_ADMIN)) {
            throw new BadRequestException("You cannot deactivate or demote your own account");
        }

        AdminUser admin =
                adminUserRepository.findById(id).orElseThrow(() -> new NotFoundException("Admin user not found: " + id));

        // Prevent escalating to SUPER_ADMIN
        if (req.role() == AdminRole.SUPER_ADMIN && admin.getRole() != AdminRole.SUPER_ADMIN) {
            throw new BadRequestException("Cannot promote an account to SUPER_ADMIN through this endpoint");
        }

        // SUB_ADMIN must have a valid country code
        if (req.role() == AdminRole.SUB_ADMIN) {
            if (req.countryCode() == null || req.countryCode().isBlank()
                    || !VALID_COUNTRY_CODES.contains(req.countryCode().toUpperCase())) {
                throw new BadRequestException(
                        "SUB_ADMIN requires a valid country code. Allowed: IN, AE, US, SG, LK");
            }
        }

        boolean roleOrCountryChanged = admin.getRole() != req.role()
                || !String.valueOf(admin.getCountryCode()).equals(String.valueOf(req.countryCode()));

        admin.setName(req.name());
        admin.setRole(req.role());
        admin.setActive(req.active());
        admin.setPhone(blankToNull(req.phone()));
        admin.setModules(sanitize(req.modules(), VALID_MODULES));
        admin.setPermissions(sanitize(req.permissions(), VALID_PERMISSIONS));
        if (req.countryCode() != null && !req.countryCode().isBlank()) {
            admin.setCountryCode(req.countryCode().toUpperCase());
        } else {
            admin.setCountryCode(null);
        }

        AdminUser saved = adminUserRepository.save(admin);

        // If role or country changed, revoke all existing sessions for security
        if (roleOrCountryChanged) {
            sessionService.revokeAllSessions(id);
        }

        return toDto(saved);
    }

    /**
     * Permanently removes an admin account.
     *
     * <p>Two guards protect the super-admin seat: the caller cannot delete their own account,
     * and no SUPER_ADMIN row may be removed through this endpoint at all — so the panel can
     * never lock itself out.
     *
     * <p>Access passes and refresh tokens are dropped with the account (FK CASCADE); the
     * login-audit trail keeps its rows with {@code admin_id} nulled, so what this account
     * actually did stays readable after it is gone.
     */
    public void delete(UUID id, UUID requestingAdminId, HttpServletRequest request) {
        if (id.equals(requestingAdminId)) {
            throw new BadRequestException("You cannot delete your own account");
        }
        AdminUser admin = adminUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Admin user not found: " + id));
        if (admin.getRole() == AdminRole.SUPER_ADMIN) {
            throw new BadRequestException("SUPER_ADMIN accounts cannot be deleted");
        }

        String email = admin.getEmail();
        // Revoke live sessions before the row goes — a still-valid JWT must not
        // outlive the account it belongs to.
        sessionService.revokeAllSessions(id);
        adminUserRepository.delete(admin);
        adminAuditService.recordSecurityEvent(email, "ADMIN_USER_DELETED", request);
    }

    private AdminUserSummaryDto toDto(AdminUser a, Instant lastLogin) {
        return new AdminUserSummaryDto(
                a.getId(), a.getEmail(), a.getName(), a.getRole(), a.isActive(), a.getCountryCode(),
                a.getPhone(), AdminAccessService.splitCsv(a.getModules()),
                AdminAccessService.splitCsv(a.getPermissions()), lastLogin);
    }

    /** Keeps only known keys, lowercased, deduped; null when nothing valid remains (legacy full access). */
    private static String sanitize(List<String> values, Set<String> allowed) {
        if (values == null) {
            return null;
        }
        String joined = values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.trim().toLowerCase())
                .filter(allowed::contains)
                .distinct()
                .collect(java.util.stream.Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Single-row path (create/update) — one lookup is fine outside the list. */
    private AdminUserSummaryDto toDto(AdminUser a) {
        Instant lastLogin = adminLoginAuditRepository.findLatestSuccessfulLoginByAdminId(a.getId()).orElse(null);
        return toDto(a, lastLogin);
    }
}
