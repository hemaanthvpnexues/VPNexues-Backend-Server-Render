package com.vpnexues.svc.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminCustomerDto(
        UUID id,
        String name,
        String email,
        String phone,
        String countryCode,
        int orderCount,
        BigDecimal totalSpent,
        String customerType,
        Instant registeredAt) {
}
