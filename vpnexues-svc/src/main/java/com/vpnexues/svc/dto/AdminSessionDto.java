package com.vpnexues.svc.dto;

import com.vpnexues.svc.entity.AdminRole;
import java.util.List;
import java.util.UUID;

/** Session payload for the admin SPA. modules/permissions drive per-tab access for non-super admins. */
public record AdminSessionDto(
        UUID id,
        String email,
        String name,
        AdminRole role,
        String countryCode,
        List<String> modules,
        List<String> permissions) {
}
