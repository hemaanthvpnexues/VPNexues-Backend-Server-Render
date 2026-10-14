package com.vpnexues.svc.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateStoreSettingsRequest(
        String websiteName,
        String tagline,
        @NotBlank String defaultCurrency,
        String timezone,
        String dateFormat,
        String timeFormat,
        String websiteEmail,
        String websitePhone,
        String storeName,
        String storeEmail,
        String storePhone,
        String storeAddress,
        String storeDescription,
        BigDecimal freeShippingThreshold,
        BigDecimal standardShippingRate,
        BigDecimal expressShippingRate,
        Integer defaultDeliveryDays,
        String facebookUrl,
        String instagramUrl,
        String twitterUrl,
        String youtubeUrl,
        String whatsappUrl,
        String telegramUrl,
        // Pricing knobs — bounds keep a fat-fingered/SUB_ADMIN edit from rewriting the whole
        // price list (e.g. discountRate 0.99 = 99% off every Big Box; negative rates invert
        // the +15% rule). Sensible ranges around the seeded defaults (10 / 0.15 / 10 / 0.12).
        @NotNull @Min(1) @Max(1000) Integer smallBoxStandardSlots,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal smallBoxPremiumRate,
        @NotNull @DecimalMin("0.0") @DecimalMax("1000") BigDecimal bigBoxMinKg,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal bigBoxDiscountRate) {
}
