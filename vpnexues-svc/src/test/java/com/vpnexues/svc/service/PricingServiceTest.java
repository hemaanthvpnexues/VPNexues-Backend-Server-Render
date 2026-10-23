package com.vpnexues.svc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vpnexues.svc.dto.BoxSummaryDto;
import com.vpnexues.svc.entity.CartItem;
import com.vpnexues.svc.entity.StoreSettings;
import com.vpnexues.svc.repository.ProductPriceOverrideRepository;
import com.vpnexues.svc.repository.StoreSettingsRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The two pricing rules the admin Store tab now edits live through
 * PUT /api/admin/settings (AGENTS.md test-writer scope):
 *
 * <ul>
 *   <li>Small Box: first N units standard, every unit after that at +premium rate.</li>
 *   <li>Big Box: total kg &gt;= minKg takes discountRate off the WHOLE subtotal (cliff,
 *       not marginal).</li>
 * </ul>
 *
 * Every case proves the store_settings knobs really change the math — the guarantee
 * behind the Settings page's four pricing inputs — plus the fallback defaults used
 * when the row (or a field on it) is missing.
 */
class PricingServiceTest {

    private StoreSettingsRepository storeSettingsRepository;
    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        storeSettingsRepository = mock(StoreSettingsRepository.class);
        pricingService = new PricingService(
                mock(ProductPriceOverrideRepository.class), storeSettingsRepository);
    }

    /** Configure the single store_settings row the service reads for every computation. */
    private void withSettings(String slots, String premiumRate, String minKg, String discountRate) {
        StoreSettings s = new StoreSettings();
        s.setSmallBoxStandardSlots(Integer.valueOf(slots));
        s.setSmallBoxPremiumRate(new BigDecimal(premiumRate));
        s.setBigBoxMinKg(new BigDecimal(minKg));
        s.setBigBoxDiscountRate(new BigDecimal(discountRate));
        when(storeSettingsRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(s));
    }

    private void withNoSettingsRow() {
        when(storeSettingsRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());
    }

    private static CartItem item(int qty, String unitPrice, String weightKg) {
        CartItem item = new CartItem();
        item.setQty(qty);
        item.setUnitPriceSnapshot(new BigDecimal(unitPrice));
        item.setWeightKg(weightKg == null ? null : new BigDecimal(weightKg));
        return item;
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual);
    }

    @Test
    void smallBoxChargesStandardForFirstTenThenPremium() {
        withSettings("10", "0.15", "10", "0.12");

        PricingService.BoxPricingResult result = pricingService.computeSmallBoxPricing(
                List.of(item(10, "2.00", null), item(1, "2.00", null)));

        // Line 1 fills all 10 standard slots; the 11th unit crosses lines at +15%.
        assertMoney("20.00", result.lines().get(0).lineTotal());
        assertMoney("2.30", result.lines().get(1).lineTotal());

        BoxSummaryDto summary = result.summary();
        assertEquals("SMALL_BOX", summary.boxType());
        assertEquals(0, new BigDecimal("11").compareTo(summary.progressValue()));
        assertEquals(0, new BigDecimal("10").compareTo(summary.threshold()));
        assertTrue(summary.minimumMet());
        assertMoney("22.30", summary.subtotal());
        assertMoney("0.30", summary.premiumOrDiscount());
        assertMoney("22.30", summary.total());
    }

    @Test
    void smallBoxHonoursCustomSlotsAndPremiumRateFromSettings() {
        // Same rule, different knobs from store_settings: only 3 standard slots, +10% after.
        withSettings("3", "0.10", "10", "0.12");

        PricingService.BoxPricingResult result = pricingService.computeSmallBoxPricing(
                List.of(item(4, "10.00", null)));

        assertMoney("41.00", result.lines().get(0).lineTotal()); // 3×10.00 + 1×11.00
        assertMoney("1.00", result.summary().premiumOrDiscount());
        assertEquals(0, new BigDecimal("3").compareTo(result.summary().threshold()));
        assertTrue(result.summary().minimumMet());
    }

    @Test
    void smallBoxBelowThresholdChargesNoPremium() {
        withSettings("10", "0.15", "10", "0.12");

        PricingService.BoxPricingResult result = pricingService.computeSmallBoxPricing(
                List.of(item(9, "2.00", null)));

        assertMoney("18.00", result.lines().get(0).lineTotal());
        assertMoney("0.00", result.summary().premiumOrDiscount());
        assertFalse(result.summary().minimumMet());
    }

    @Test
    void smallBoxFallsBackToDefaultsWhenSettingsRowMissing() {
        withNoSettingsRow();

        PricingService.BoxPricingResult result = pricingService.computeSmallBoxPricing(
                List.of(item(11, "2.00", null)));

        assertMoney("22.30", result.lines().get(0).lineTotal()); // 10 standard + 1 × 1.15
        assertEquals(0, new BigDecimal("10").compareTo(result.summary().threshold()));
        assertMoney("0.30", result.summary().premiumOrDiscount());
    }

    @Test
    void bigBoxCliffDiscountAppliesOnceMinKgReached() {
        withSettings("10", "0.15", "10", "0.12");

        PricingService.BoxPricingResult result = pricingService.computeBigBoxPricing(
                List.of(item(1, "5.00", "12")));

        // 12kg >= 10kg → 12% off the whole subtotal; the line itself stays undiscounted.
        assertMoney("60.00", result.lines().get(0).lineTotal());
        assertMoney("60.00", result.summary().subtotal());
        assertTrue(result.summary().minimumMet());
        assertMoney("7.20", result.summary().premiumOrDiscount());
        assertMoney("52.80", result.summary().total());
    }

    @Test
    void bigBoxCustomMinKgAndRateShiftTheCliff() {
        withSettings("10", "0.15", "20", "0.25");

        PricingService.BoxPricingResult below = pricingService.computeBigBoxPricing(
                List.of(item(1, "5.00", "12")));
        // 12kg < configured 20kg → no discount even though 12kg meets the default.
        assertFalse(below.summary().minimumMet());
        assertMoney("0.00", below.summary().premiumOrDiscount());
        assertMoney("60.00", below.summary().total());

        PricingService.BoxPricingResult atCliff = pricingService.computeBigBoxPricing(
                List.of(item(1, "5.00", "20")));
        // 20kg → 25% off 100.00.
        assertTrue(atCliff.summary().minimumMet());
        assertMoney("25.00", atCliff.summary().premiumOrDiscount());
        assertMoney("75.00", atCliff.summary().total());
    }

    @Test
    void bigBoxFallsBackToDefaultsWhenSettingsRowMissing() {
        withNoSettingsRow();

        PricingService.BoxPricingResult result = pricingService.computeBigBoxPricing(
                List.of(item(1, "5.00", "12")));

        assertEquals(0, new BigDecimal("10").compareTo(result.summary().threshold()));
        assertMoney("7.20", result.summary().premiumOrDiscount()); // 12% of 60.00
        assertMoney("52.80", result.summary().total());
    }
}
