package com.vpnexues.svc.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequest(
        @NotBlank String name,
        String slug,
        @NotBlank String category,
        String description,
        @NotNull @DecimalMin("0.0") BigDecimal basePrice,
        BigDecimal oldPrice,
        @NotBlank String unit,
        @NotBlank String sku,
        String imageUrl,
        Integer discountPct,
        List<String> badges,
        /**
         * Optional market code (IN/SG/US/AE/...). When present, {@code basePrice} is that
         * region's price: an override pins it for the shop and the SGD base is derived from it.
         * Absent = treat {@code basePrice} as the SGD base (legacy behavior).
         */
        String countryCode) {
}
