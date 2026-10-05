package com.vpnexues.svc.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
        Instant createdAt,
        /**
         * Effective per-market price keyed by region code (IN/SG/US/AE + any market with an
         * override), computed exactly like the shop: price override if set, else base × rate.
         * Lets admin dashboards show the same numbers customers see, in every region.
         */
        Map<String, RegionalPrice> prices) {

    public record RegionalPrice(BigDecimal price, BigDecimal oldPrice) {}
}
