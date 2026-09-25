package com.vpnexues.svc.dto;

import java.math.BigDecimal;

public record OrderItemDto(
        String name,
        String imageUrl,
        String boxType,
        int qty,
        BigDecimal weightKg,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {
}
