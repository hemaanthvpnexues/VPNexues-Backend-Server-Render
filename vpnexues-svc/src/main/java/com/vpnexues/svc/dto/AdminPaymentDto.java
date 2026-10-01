package com.vpnexues.svc.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminPaymentDto(
        UUID id,
        UUID orderId,
        String orderNumber,
        String customer,
        String customerPhone,
        String customerEmail,
        String countryCode,
        String method,
        String status,
        BigDecimal amount,
        Instant date,
        String orderStatus) {
}
