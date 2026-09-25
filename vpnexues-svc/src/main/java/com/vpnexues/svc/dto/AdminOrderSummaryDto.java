package com.vpnexues.svc.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminOrderSummaryDto(
        UUID id,
        String orderNumber,
        String buyerName,
        String buyerPhone,
        String countryCode,
        String channel,
        String status,
        BigDecimal grandTotal,
        Instant createdAt) {
}
