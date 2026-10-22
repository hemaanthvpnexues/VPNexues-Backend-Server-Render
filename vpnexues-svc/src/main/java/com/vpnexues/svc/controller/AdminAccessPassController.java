package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.AdminAccessPassDto;
import com.vpnexues.svc.entity.AdminUser;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.AdminUserRepository;
import com.vpnexues.svc.service.AdminAccessPassService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Feature B — Super Admin side of the one-time access pass.
 *
 * <p>All four endpoints sit under {@code /api/admin/**} (admin JWT cookie chain) and are
 * additionally {@code SUPER_ADMIN}-gated at the class level, same as
 * {@link AdminUserManagementController}: only the Super Admin may mint, read, audit or kill
 * a pass. The consuming side (verify) lives inside the anonymous login flow in
 * {@link AdminAuthController}, not here.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminAccessPassController {

    private final AdminAccessPassService accessPassService;
    private final AdminUserRepository adminUserRepository;

    /** Issue a pass — the response is the one and only time the code is ever returned. */
    @PostMapping("/api/admin/admin-users/{id}/access-pass")
    public AdminAccessPassDto generate(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID requestingAdminId,
            HttpServletRequest request) {
        AdminUser target = requireAdmin(id);
        AdminUser issuedBy = requireAdmin(requestingAdminId);
        return accessPassService.generate(target, issuedBy, request);
    }

    /** Current state for the countdown UI — deliberately code-free. */
    @GetMapping("/api/admin/admin-users/{id}/access-pass")
    public AdminAccessPassDto active(@PathVariable UUID id) {
        return accessPassService.active(id);
    }

    /** Audit trail: who issued it, for whom, when used, from which IP. */
    @GetMapping("/api/admin/admin-users/{id}/access-pass/history")
    public List<AdminAccessPassDto> history(@PathVariable UUID id) {
        requireAdmin(id);
        return accessPassService.history(id);
    }

    /** Instant revocation of an un-used pass. */
    @DeleteMapping("/api/admin/admin-users/{id}/access-pass")
    public ResponseEntity<Void> revoke(@PathVariable UUID id, HttpServletRequest request) {
        requireAdmin(id);
        accessPassService.invalidate(id, request);
        return ResponseEntity.noContent().build();
    }

    private AdminUser requireAdmin(UUID id) {
        if (id == null) {
            throw new NotFoundException("Admin not found");
        }
        return adminUserRepository.findById(id).orElseThrow(() -> new NotFoundException("Admin not found"));
    }
}
