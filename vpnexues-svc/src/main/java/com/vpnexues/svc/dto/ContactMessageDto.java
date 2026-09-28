package com.vpnexues.svc.dto;

import java.time.Instant;
import java.util.UUID;

public record ContactMessageDto(
        UUID id,
        String name,
        String email,
        String phone,
        String countryCode,
        String subject,
        String message,
        String status,
        Instant createdAt) {
}
