package com.vpnexues.svc.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemDto(
        UUID id,
        UUID productId,
        String name,
        String imageUrl,
        String unit,
        String boxType,
        int qty,
        BigDecimal weightKg,
        BigDecimal unitPrice,
        BigDecimal oldPrice,
        BigDecimal lineTotal) {
}
