package com.vpnexues.svc.dto;

import com.vpnexues.svc.entity.AdminRole;
import java.util.UUID;

public record AdminSessionDto(UUID id, String email, String name, AdminRole role, String countryCode) {
}
