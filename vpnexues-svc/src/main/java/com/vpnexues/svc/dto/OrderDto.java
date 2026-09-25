package com.vpnexues.svc.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDto(
        UUID id,
        String orderNumber,
        String status,
        String channel,
        BigDecimal itemTotal,
        BigDecimal discount,
        BigDecimal boxDiscount,
        BigDecimal deliveryFee,
        BigDecimal tax,
        BigDecimal tip,
        BigDecimal grandTotal,
        String paymentMethod,
        String couponCode,
        List<OrderItemDto> items,
        Instant createdAt) {
}
