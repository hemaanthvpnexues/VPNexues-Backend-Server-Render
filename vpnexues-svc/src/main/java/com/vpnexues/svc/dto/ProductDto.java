package com.vpnexues.svc.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductDto(
        UUID id,
        String name,
        String slug,
        String category,
        String description,
        BigDecimal price,
        BigDecimal oldPrice,
        String unit,
        String sku,
        String imageUrl,
        Integer discountPct,
        List<String> badges) {
}
