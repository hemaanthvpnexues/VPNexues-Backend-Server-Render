package com.vpnexues.svc.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** weightKg is only meaningful for a BIG_BOX line; ignored (qty-only update) otherwise. */
public record UpdateCartItemRequest(@Positive int qty, @PositiveOrZero BigDecimal weightKg) {
}
