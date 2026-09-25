package com.vpnexues.svc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CheckoutRequest(
        @NotBlank String addressId,
        @NotBlank String paymentMethod,
        String couponCode,
        @PositiveOrZero BigDecimal tip) {
}
