package com.vpnexues.svc.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderFeedbackDto(
        UUID id,
        String orderNumber,
        int rating,
        List<String> tags,
        String comment,
        String headline,
        String countryCode,
        String contactEmail,
        String contactPhone,
        Instant createdAt) {}
