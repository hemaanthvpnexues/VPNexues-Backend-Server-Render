package com.vpnexues.svc.dto;

import java.util.UUID;

public record CategoryDto(UUID id, String name, String slug, String emoji, boolean active) {
}
