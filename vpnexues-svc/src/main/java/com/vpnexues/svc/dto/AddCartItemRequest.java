package com.vpnexues.svc.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * boxType/weightKg are optional — plain product adds (boxType null) behave exactly as before.
 * When boxType is SMALL_BOX or BIG_BOX, {@link com.vpnexues.svc.service.CartService#setBoxItem} is
 * used instead of the plain increment-based add: qty/weightKg are treated as the new absolute
 * value for that product+boxType line, not an increment.
 */
public record AddCartItemRequest(
        @NotNull UUID productId, @Positive int qty, String boxType, @PositiveOrZero BigDecimal weightKg) {
}
