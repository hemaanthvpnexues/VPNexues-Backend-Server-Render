package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.AdminUserSummaryDto;
import com.vpnexues.svc.dto.CreateAdminUserRequest;
import com.vpnexues.svc.dto.UpdateAdminUserRequest;
import com.vpnexues.svc.service.AdminUserManagementService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Every endpoint here is SUPER_ADMIN-only — managing who else gets admin access. */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUserManagementController {

    private final AdminUserManagementService adminUserManagementService;

    @GetMapping("/api/admin/admin-users")
    public List<AdminUserSummaryDto> list() {
        return adminUserManagementService.list();
    }

    @PostMapping("/api/admin/admin-users")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserSummaryDto create(@RequestBody @Valid CreateAdminUserRequest req) {
        return adminUserManagementService.create(req);
    }

    @PutMapping("/api/admin/admin-users/{id}")
    public AdminUserSummaryDto update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateAdminUserRequest req,
            @AuthenticationPrincipal UUID requestingAdminId) {
        return adminUserManagementService.update(id, req, requestingAdminId);
    }
}
