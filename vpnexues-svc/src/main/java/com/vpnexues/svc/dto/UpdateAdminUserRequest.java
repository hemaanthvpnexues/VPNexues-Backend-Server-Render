package com.vpnexues.svc.dto;

import com.vpnexues.svc.entity.AdminRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateAdminUserRequest(
        @NotBlank String name,
        @NotNull AdminRole role,
        boolean active,
        String countryCode,
        String phone,
        /** Module keys: dashboard, orders, products, users, reports, payments, support, content. */
        List<String> modules,
        /** Action keys: view, edit, delete, export. */
        List<String> permissions) {
}
