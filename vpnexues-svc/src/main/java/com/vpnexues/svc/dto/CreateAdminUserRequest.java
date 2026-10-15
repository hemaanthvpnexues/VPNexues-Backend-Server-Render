package com.vpnexues.svc.dto;

import com.vpnexues.svc.entity.AdminRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateAdminUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String name,
        @NotNull AdminRole role,
        String countryCode,
        String phone,
        /** Module keys: dashboard, orders, products, users, reports, payments, support, content. */
        List<String> modules,
        /** Action keys: view, edit, delete, export. */
        List<String> permissions) {
}
