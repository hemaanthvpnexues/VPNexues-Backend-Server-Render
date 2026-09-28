package com.vpnexues.svc.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatSessionDto(
        UUID id,
        String visitorName,
        String visitorEmail,
        String countryCode,
        String subject,
        String status,
        Instant createdAt,
        Instant closedAt,
        List<ChatMessageDto> messages) {
}
