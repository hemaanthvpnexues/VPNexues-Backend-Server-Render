package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.BoxSummaryDto;
import com.vpnexues.svc.entity.CartItem;
import com.vpnexues.svc.entity.ProductPriceOverride;
import com.vpnexues.svc.entity.Product;
import com.vpnexues.svc.entity.StoreSettings;
import com.vpnexues.svc.repository.ProductPriceOverrideRepository;
import com.vpnexues.svc.repository.StoreSettingsRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Resolves the effective price of a Product for a given market: override if one exists, else base price. */
@Service
@RequiredArgsConstructor
public class PricingService {

    /** Fallbacks used only if store_settings somehow has a null rate/threshold (shouldn't happen post-migration). */
    private static final int DEFAULT_SMALL_BOX_STANDARD_SLOTS = 10;
    private static final BigDecimal DEFAULT_SMALL_BOX_PREMIUM_RATE = new BigDecimal("0.15");
    private static final BigDecimal DEFAULT_BIG_BOX_MIN_KG = BigDecimal.TEN;
    private static final BigDecimal DEFAULT_BIG_BOX_DISCOUNT_RATE = new BigDecimal("0.12");

    /** Markets with explicit conversion rates — the four regions admin dashboards must mirror. */
    public static final List<String> PRICED_REGIONS = List.of("IN", "SG", "US", "AE");

    private final ProductPriceOverrideRepository priceOverrideRepository;
    private final StoreSettingsRepository storeSettingsRepository;

    private StoreSettings settings() {
        return storeSettingsRepository.findFirstByOrderByCreatedAtAsc().orElse(null);
    }

    /** Small Box: first N units (by add order, across all lines) at standard price, unit N+1 at +premium rate. */
    private int smallBoxStandardSlots(StoreSettings s) {
        return s != null && s.getSmallBoxStandardSlots() != null ? s.getSmallBoxStandardSlots() : DEFAULT_SMALL_BOX_STANDARD_SLOTS;
    }

    private BigDecimal smallBoxPremiumMultiplier(StoreSettings s) {
        BigDecimal rate = s != null && s.getSmallBoxPremiumRate() != null ? s.getSmallBoxPremiumRate() : DEFAULT_SMALL_BOX_PREMIUM_RATE;
        return BigDecimal.ONE.add(rate);
    }

    /** Big Box: reaching the configured min kg (cliff, not marginal) applies the configured discount rate off the entire subtotal. */
    private BigDecimal bigBoxMinKg(StoreSettings s) {
        return s != null && s.getBigBoxMinKg() != null ? s.getBigBoxMinKg() : DEFAULT_BIG_BOX_MIN_KG;
    }

    private BigDecimal bigBoxDiscountRate(StoreSettings s) {
        return s != null && s.getBigBoxDiscountRate() != null ? s.getBigBoxDiscountRate() : DEFAULT_BIG_BOX_DISCOUNT_RATE;
    }

    public record ResolvedPrice(BigDecimal price, BigDecimal oldPrice) {
    }

    public ResolvedPrice resolve(Product product, String countryCode, ProductPriceOverride preloaded) {
        if (preloaded != null) {
            return new ResolvedPrice(preloaded.getPrice(), preloaded.getOldPrice());
        }
        return resolve(product, countryCode);
    }

    public ResolvedPrice resolve(Product product, String countryCode) {
        return priceOverrideRepository
                .findByProductIdAndCountryCode(product.getId(), countryCode)
                .map(o -> new ResolvedPrice(o.getPrice(), o.getOldPrice()))
                .orElseGet(() -> resolveLoaded(product, countryCode, null));
    }

    /** SGD base → market rate (so US/AE see correct local price). Unknown markets fall back to 1 (SG). */
    public BigDecimal rateFor(String countryCode) {
        return switch (countryCode != null ? countryCode.toUpperCase() : "SG") {
            case "IN" -> new BigDecimal("62");
            case "SG" -> BigDecimal.ONE;
            case "US" -> new BigDecimal("0.74");
            case "AE" -> new BigDecimal("2.72");
            default -> BigDecimal.ONE;
        };
    }

    /**
     * Resolve with a preloaded override (null = none) and NO repository lookup — used by admin
     * product lists that batch-load overrides, so 1000 products don't trigger 1000 queries.
     */
    public ResolvedPrice resolveLoaded(Product product, String countryCode, ProductPriceOverride preloaded) {
        if (preloaded != null) {
            return new ResolvedPrice(preloaded.getPrice(), preloaded.getOldPrice());
        }
        BigDecimal rate = rateFor(countryCode);
        BigDecimal price = product.getBasePrice().multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal old = product.getOldPrice() != null
                ? product.getOldPrice().multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP)
                : null;
        return new ResolvedPrice(price, old);
    }

    /** Per-line result for a box pricing computation: how much of a CartItem's lineTotal to charge. */
    public record BoxLinePricing(CartItem item, BigDecimal lineTotal) {
    }

    public record BoxPricingResult(List<BoxLinePricing> lines, BoxSummaryDto summary) {
    }

    /**
     * Small Box pricing: {@code items} must already be sorted oldest-first (createdAt ascending) —
     * that insertion order determines which units land in the first 10 (standard) vs. 11+ (premium)
     * slots, matching the reference builder's cross-product FIFO behavior exactly.
     */
    public BoxPricingResult computeSmallBoxPricing(List<CartItem> items) {
        StoreSettings s = settings();
        int standardSlots = smallBoxStandardSlots(s);
        BigDecimal premiumMultiplier = smallBoxPremiumMultiplier(s);

        List<BoxLinePricing> lines = new ArrayList<>();
        int slot = 0;
        int totalUnits = 0;
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal premiumPortion = BigDecimal.ZERO;
        for (CartItem item : items) {
            BigDecimal unitPrice = item.getUnitPriceSnapshot();
            BigDecimal lineTotal = BigDecimal.ZERO;
            for (int i = 0; i < item.getQty(); i++) {
                boolean standard = slot < standardSlots;
                BigDecimal unitCharge = standard ? unitPrice : unitPrice.multiply(premiumMultiplier);
                if (!standard) {
                    premiumPortion = premiumPortion.add(unitCharge.subtract(unitPrice));
                }
                lineTotal = lineTotal.add(unitCharge);
                slot++;
                totalUnits++;
            }
            subtotal = subtotal.add(lineTotal);
            lines.add(new BoxLinePricing(item, lineTotal));
        }
        boolean minimumMet = totalUnits >= standardSlots;
        BoxSummaryDto summary = new BoxSummaryDto(
                "SMALL_BOX",
                BigDecimal.valueOf(totalUnits),
                BigDecimal.valueOf(standardSlots),
                minimumMet,
                subtotal,
                premiumPortion,
                subtotal);
        return new BoxPricingResult(lines, summary);
    }

    /** Big Box pricing: weight-based, configured cliff discount off the whole subtotal once total kg >= configured min. */
    public BoxPricingResult computeBigBoxPricing(List<CartItem> items) {
        StoreSettings s = settings();
        BigDecimal minKg = bigBoxMinKg(s);
        BigDecimal discountRate = bigBoxDiscountRate(s);

        List<BoxLinePricing> undiscountedLines = new ArrayList<>();
        BigDecimal kg = BigDecimal.ZERO;
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            BigDecimal weight = item.getWeightKg() != null ? item.getWeightKg() : BigDecimal.ZERO;
            BigDecimal lineTotal = item.getUnitPriceSnapshot().multiply(weight);
            kg = kg.add(weight);
            subtotal = subtotal.add(lineTotal);
            undiscountedLines.add(new BoxLinePricing(item, lineTotal));
        }
        boolean minimumMet = kg.compareTo(minKg) >= 0;
        BigDecimal discount = minimumMet ? subtotal.multiply(discountRate) : BigDecimal.ZERO;
        BigDecimal total = subtotal.subtract(discount);
        BoxSummaryDto summary =
                new BoxSummaryDto("BIG_BOX", kg, minKg, minimumMet, subtotal, discount, total);
        // Big Box's 12% is an aggregate-only adjustment (matches the coupon-discount pattern) — each
        // line's own total stays undiscounted so itemTotal = sum(lineTotal) remains simple/consistent.
        return new BoxPricingResult(undiscountedLines, summary);
    }

    /** Groups cart items into regular / SMALL_BOX / BIG_BOX buckets, SMALL_BOX sorted oldest-first. */
    public Map<String, List<CartItem>> groupByBoxType(List<CartItem> items) {
        Map<String, List<CartItem>> grouped = new java.util.HashMap<>();
        for (CartItem item : items) {
            String key = item.getBoxType() == null ? "" : item.getBoxType();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }
        List<CartItem> smallBox = grouped.get("SMALL_BOX");
        if (smallBox != null) {
            smallBox.sort(Comparator.comparing(CartItem::getCreatedAt));
        }
        return grouped;
    }
}
