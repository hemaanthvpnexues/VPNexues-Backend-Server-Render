package com.vpnexues.svc.dto;

import com.vpnexues.svc.entity.AdminRole;
import java.util.UUID;

public record AdminUserSummaryDto(UUID id, String email, String name, AdminRole role, boolean active, String countryCode) {
}
