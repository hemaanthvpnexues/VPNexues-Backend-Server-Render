package com.vpnexues.svc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vpnexues.svc.dto.ProductDto;
import com.vpnexues.svc.entity.Product;
import com.vpnexues.svc.entity.ProductPriceOverride;
import com.vpnexues.svc.repository.ProductPriceOverrideRepository;
import com.vpnexues.svc.repository.ProductRepository;
import com.vpnexues.svc.repository.StoreSettingsRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Regression for the N+1 price-override query in {@code GET /api/products}
 * (performance report, 13-Oct-2026): {@code list()} batch-loads every override in ONE
 * query, but the old toDto() fell through to a per-product repository lookup whenever a
 * product had no override (the normal case) — ~24 extra queries x ~300ms against the
 * Supabase pooler = 4-9s on every shop/category/search load.
 *
 * <p>Guards three things:
 * <ol>
 *   <li>the batch path never calls {@code findByProductIdAndCountryCode} (N+1 is gone),</li>
 *   <li>prices are still correct — manual country overrides win, base x rate otherwise,</li>
 *   <li>{@code getBySlug} still DOES the lookup (18 products have manual INR overrides;
 *       it must never start showing base prices on the product detail page).</li>
 * </ol>
 */
class ProductServiceTest {

    private ProductRepository productRepository;
    private ProductPriceOverrideRepository priceOverrideRepository;
    private ProductService productService;

    private Product ridgeGourd;
    private Product tomato;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        priceOverrideRepository = mock(ProductPriceOverrideRepository.class);
        StoreSettingsRepository storeSettingsRepository = mock(StoreSettingsRepository.class);
        when(storeSettingsRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());

        // Real PricingService (not a mock) so the actual base-price x rate math is asserted.
        PricingService pricingService = new PricingService(priceOverrideRepository, storeSettingsRepository);
        productService = new ProductService(productRepository, priceOverrideRepository, pricingService);

        ridgeGourd = product("Ridge Gourd", "15.00", "20.00");
        tomato = product("Tomato", "4.50", null);

        when(priceOverrideRepository.findByProductIdAndCountryCode(any(UUID.class), anyString()))
                .thenReturn(Optional.empty());
    }

    private Product product(String name, String basePrice, String oldPrice) {
        Product p = new Product();
        p.setId(UUID.randomUUID());
        p.setName(name);
        p.setSlug(name.toLowerCase().replace(' ', '-'));
        p.setCategory("Vegetables");
        p.setDescription(name);
        p.setBasePrice(new BigDecimal(basePrice));
        p.setOldPrice(oldPrice != null ? new BigDecimal(oldPrice) : null);
        p.setUnit("per kg");
        p.setActive(true);
        return p;
    }

    private ProductPriceOverride override(Product p, String countryCode, String price, String oldPrice) {
        ProductPriceOverride o = new ProductPriceOverride();
        o.setProduct(p);
        o.setCountryCode(countryCode);
        o.setPrice(new BigDecimal(price));
        o.setOldPrice(oldPrice != null ? new BigDecimal(oldPrice) : null);
        return o;
    }

    @Test
    void list_withoutOverrides_doesOneBatchedQueryAndNoPerProductLookup() {
        List<Product> products = List.of(ridgeGourd, tomato);
        when(productRepository.findByActiveTrueOrderBySalesCountDesc(any()))
                .thenReturn(new PageImpl<>(products, PageRequest.of(0, 24), products.size()));
        when(priceOverrideRepository.findByCountryCodeAndProductIdIn(eq("IN"), any()))
                .thenReturn(List.of());

        List<ProductDto> dtos = productService
                .list(null, null, "IN", null, PageRequest.of(0, 24))
                .getContent();

        assertEquals(2, dtos.size());
        // THE regression: zero per-product override queries for the whole page.
        verify(priceOverrideRepository, times(1)).findByCountryCodeAndProductIdIn(eq("IN"), any());
        verify(priceOverrideRepository, never()).findByProductIdAndCountryCode(any(UUID.class), anyString());
        // Pricing still resolves: IN rate = 62 (SGD base x rate), old price scaled too.
        assertEquals(0, new BigDecimal("930.00").compareTo(dtos.get(0).price()));   // 15.00 x 62
        assertEquals(0, new BigDecimal("1240.00").compareTo(dtos.get(0).oldPrice())); // 20.00 x 62
        assertEquals(0, new BigDecimal("279.00").compareTo(dtos.get(1).price()));   // 4.50 x 62
    }

    @Test
    void list_withManualOverride_appliesOverrideAndStillNoPerProductQuery() {
        List<Product> products = List.of(ridgeGourd, tomato);
        when(productRepository.findByActiveTrueOrderBySalesCountDesc(any()))
                .thenReturn(new PageImpl<>(products, PageRequest.of(0, 24), products.size()));
        // Manual INR override on ridge gourd only; tomato has none.
        when(priceOverrideRepository.findByCountryCodeAndProductIdIn(eq("IN"), any()))
                .thenReturn(List.of(override(ridgeGourd, "IN", "999.00", "1100.00")));

        List<ProductDto> dtos = productService
                .list(null, null, "IN", null, PageRequest.of(0, 24))
                .getContent();

        assertEquals(0, new BigDecimal("999.00").compareTo(dtos.get(0).price()));
        assertEquals(0, new BigDecimal("1100.00").compareTo(dtos.get(0).oldPrice()));
        // Base x rate for the product WITHOUT an override (resolveLoaded, no lookup).
        assertEquals(0, new BigDecimal("279.00").compareTo(dtos.get(1).price()));
        verify(priceOverrideRepository, never()).findByProductIdAndCountryCode(any(UUID.class), anyString());
    }

    @Test
    void getBySlug_stillLooksUpOverride_manualInrPriceIsNotLost() {
        when(productRepository.findBySlugAndActiveTrue("ridge-gourd"))
                .thenReturn(Optional.of(ridgeGourd));
        when(priceOverrideRepository.findByProductIdAndCountryCode(ridgeGourd.getId(), "IN"))
                .thenReturn(Optional.of(override(ridgeGourd, "IN", "174.00", "199.00")));

        ProductDto dto = productService.getBySlug("ridge-gourd", "IN");

        assertEquals(0, new BigDecimal("174.00").compareTo(dto.price()));
        assertEquals(0, new BigDecimal("199.00").compareTo(dto.oldPrice()));
        verify(priceOverrideRepository, times(1))
                .findByProductIdAndCountryCode(ridgeGourd.getId(), "IN");
    }

    @Test
    void getBySlug_withoutOverride_fallsBackToBasePriceTimesRate() {
        when(productRepository.findBySlugAndActiveTrue("tomato"))
                .thenReturn(Optional.of(tomato));

        ProductDto dto = productService.getBySlug("tomato", "IN");

        // No override in DB -> base x IN rate = 4.50 x 62 (same result as before the fix,
        // just reached without any N+1 on the list path).
        assertEquals(0, new BigDecimal("279.00").compareTo(dto.price()));
    }
}
