package com.vpnexues.svc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Single-row table (see R__Seed_data.sql, which seeds exactly one row). General/Store/Shipping/
 * Social fields only — no SMTP credentials or payment-gateway secrets, per the plan's Settings
 * scope decision (those two admin UI tabs render but don't persist anywhere yet).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "store_settings")
public class StoreSettings extends BaseEntity {

    @Column(name = "website_name")
    private String websiteName;

    private String tagline;

    @Column(name = "default_currency", nullable = false, length = 4)
    private String defaultCurrency = "SGD";

    private String timezone;

    @Column(name = "date_format")
    private String dateFormat;

    @Column(name = "time_format")
    private String timeFormat;

    @Column(name = "website_email")
    private String websiteEmail;

    @Column(name = "website_phone")
    private String websitePhone;

    @Column(name = "store_name")
    private String storeName;

    @Column(name = "store_email")
    private String storeEmail;

    @Column(name = "store_phone")
    private String storePhone;

    @Column(name = "store_address", columnDefinition = "text")
    private String storeAddress;

    @Column(name = "store_description", columnDefinition = "text")
    private String storeDescription;

    @Column(name = "free_shipping_threshold", precision = 12, scale = 2)
    private BigDecimal freeShippingThreshold;

    @Column(name = "standard_shipping_rate", precision = 12, scale = 2)
    private BigDecimal standardShippingRate;

    @Column(name = "express_shipping_rate", precision = 12, scale = 2)
    private BigDecimal expressShippingRate;

    @Column(name = "default_delivery_days")
    private Integer defaultDeliveryDays;

    @Column(name = "facebook_url")
    private String facebookUrl;

    @Column(name = "instagram_url")
    private String instagramUrl;

    @Column(name = "twitter_url")
    private String twitterUrl;

    @Column(name = "youtube_url")
    private String youtubeUrl;

    @Column(name = "whatsapp_url")
    private String whatsappUrl;

    @Column(name = "telegram_url")
    private String telegramUrl;

    @Column(name = "small_box_standard_slots", nullable = false)
    private Integer smallBoxStandardSlots = 10;

    @Column(name = "small_box_premium_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal smallBoxPremiumRate = new BigDecimal("0.15");

    @Column(name = "big_box_min_kg", nullable = false, precision = 6, scale = 2)
    private BigDecimal bigBoxMinKg = BigDecimal.TEN;

    @Column(name = "big_box_discount_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal bigBoxDiscountRate = new BigDecimal("0.12");
}
