package com.vpnexues.svc.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminOrderDetailDto(
        UUID id,
        String orderNumber,
        String status,
        String channel,
        UserDto buyer,
        AddressDto address,
        List<OrderItemDto> items,
        BigDecimal itemTotal,
        BigDecimal discount,
        BigDecimal boxDiscount,
        BigDecimal deliveryFee,
        BigDecimal tax,
        BigDecimal tip,
        BigDecimal grandTotal,
        String paymentMethod,
        String couponCode,
        String countryCode,
        Instant createdAt) {
}
