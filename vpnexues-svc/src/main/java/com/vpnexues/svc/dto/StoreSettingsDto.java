package com.vpnexues.svc.dto;

import java.math.BigDecimal;

public record StoreSettingsDto(
        String websiteName,
        String tagline,
        String defaultCurrency,
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
        Integer smallBoxStandardSlots,
        BigDecimal smallBoxPremiumRate,
        BigDecimal bigBoxMinKg,
        BigDecimal bigBoxDiscountRate) {
}
