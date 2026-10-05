package com.vpnexues.svc.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record UpdateProductRequest(
        @NotBlank String name,
        @NotBlank String category,
        String description,
        @NotNull @DecimalMin("0.0") BigDecimal basePrice,
        BigDecimal oldPrice,
        @NotBlank String unit,
        String imageUrl,
        Integer discountPct,
        List<String> badges,
        boolean active,
        /**
         * Optional market code (IN/SG/US/AE/...). When present, {@code basePrice}/{@code oldPrice}
         * are interpreted as that region's price and stored as a product_price_overrides row —
         * the exact value the shop shows for that region. Absent = edit the SGD base price.
         */
        String countryCode) {
}
