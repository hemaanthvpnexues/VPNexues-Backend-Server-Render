package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.AdminUserSummaryDto;
import com.vpnexues.svc.dto.CreateAdminUserRequest;
import com.vpnexues.svc.dto.UpdateAdminUserRequest;
import com.vpnexues.svc.entity.AdminRole;
import com.vpnexues.svc.entity.AdminUser;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.AdminUserRepository;
import java.util.List;
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

    /** Valid country codes for SUB_ADMIN roles. */
    private static final Set<String> VALID_COUNTRY_CODES = Set.of("IN", "AE", "US", "SG");

    @Transactional(readOnly = true)
    public List<AdminUserSummaryDto> list() {
        return adminUserRepository.findAll().stream().map(this::toDto).toList();
    }

    public AdminUserSummaryDto create(CreateAdminUserRequest req) {
        if (adminUserRepository.findByEmail(req.email()).isPresent()) {
            throw new BadRequestException("An admin account with email '" + req.email() + "' already exists");
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
                        "SUB_ADMIN requires a valid country code. Allowed: IN, AE, US, SG");
            }
        }

        AdminUser admin = new AdminUser();
        admin.setEmail(req.email());
        admin.setPasswordHash(passwordEncoder.encode(req.password()));
        admin.setName(req.name());
        admin.setRole(req.role());
        if (req.countryCode() != null && !req.countryCode().isBlank()) {
            admin.setCountryCode(req.countryCode().toUpperCase());
        }
        return toDto(adminUserRepository.save(admin));
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
                        "SUB_ADMIN requires a valid country code. Allowed: IN, AE, US, SG");
            }
        }

        boolean roleOrCountryChanged = admin.getRole() != req.role()
                || !String.valueOf(admin.getCountryCode()).equals(String.valueOf(req.countryCode()));

        admin.setName(req.name());
        admin.setRole(req.role());
        admin.setActive(req.active());
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

    private AdminUserSummaryDto toDto(AdminUser a) {
        return new AdminUserSummaryDto(a.getId(), a.getEmail(), a.getName(), a.getRole(), a.isActive(), a.getCountryCode());
    }
}
