package com.vpnexues.svc.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

/** weightKg is only meaningful for a BIG_BOX line; ignored (qty-only update) otherwise. */
public record UpdateCartItemRequest(
        // qty is an absolute set (not an increment), so only absurd values are dangerous —
        // capped to match AddCartItemRequest's 999-per-item ceiling.
        @Min(1) @Max(999) int qty,
        @DecimalMin("0.0") BigDecimal weightKg) {
}
