package com.vpnexues.svc.dto;

import com.vpnexues.svc.entity.AdminRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateAdminUserRequest(@NotBlank String name, @NotNull AdminRole role, boolean active, String countryCode) {
}
