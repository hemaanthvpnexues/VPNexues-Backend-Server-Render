package com.vpnexues.svc.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminProductDto(
        UUID id,
        String name,
        String slug,
        String category,
        String description,
        BigDecimal basePrice,
        BigDecimal oldPrice,
        String unit,
        String sku,
        String imageUrl,
        Integer discountPct,
        List<String> badges,
        boolean active,
        Instant createdAt) {
}
