package com.vpnexues.svc.dto;

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
        @NotNull Integer smallBoxStandardSlots,
        @NotNull BigDecimal smallBoxPremiumRate,
        @NotNull BigDecimal bigBoxMinKg,
        @NotNull BigDecimal bigBoxDiscountRate) {
}
