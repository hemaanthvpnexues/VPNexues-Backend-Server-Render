package com.vpnexues.svc.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageDto(
        UUID id,
        String sender,
        String body,
        Instant createdAt) {
}
