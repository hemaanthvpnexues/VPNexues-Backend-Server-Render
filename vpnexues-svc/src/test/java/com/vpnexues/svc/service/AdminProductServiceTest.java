package com.vpnexues.svc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vpnexues.svc.dto.UpdateProductRequest;
import com.vpnexues.svc.entity.Product;
import com.vpnexues.svc.entity.ProductPriceOverride;
import com.vpnexues.svc.repository.CategoryRepository;
import com.vpnexues.svc.repository.ProductPriceOverrideRepository;
import com.vpnexues.svc.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Regression for the "sub-admin price update wipes the product image" bug:
 * the price drawer sends only {name, category, basePrice, unit, active, countryCode},
 * so imageUrl/description/discountPct arrive as null — update() must treat those as
 * "keep existing", not "overwrite with null".
 */
class AdminProductServiceTest {

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private ProductPriceOverrideRepository priceOverrideRepository;
    private PricingService pricingService;
    private AdminProductService service;

    private Product product;
    private UUID productId;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        priceOverrideRepository = mock(ProductPriceOverrideRepository.class);
        pricingService = mock(PricingService.class);
        service = new AdminProductService(
                productRepository,
                categoryRepository,
                mock(InventoryItemService.class),
                priceOverrideRepository,
                pricingService);

        productId = UUID.randomUUID();
        product = new Product();
        product.setId(productId);
        product.setName("Ridge Gourd");
        product.setSlug("ridge-gourd");
        product.setCategory("Vegetables");
        product.setDescription("Farm-fresh ridge gourd");
        product.setBasePrice(new BigDecimal("15.00"));
        product.setUnit("per pc");
        product.setSku("GPNX-047");
        product.setImageUrl("https://res.cloudinary.com/eqjshlur/image/upload/v1790001287/vpnexues/Ridge%20Gourd.jpg");
        product.setDiscountPct(21);
        product.setActive(true);

        when(categoryRepository.existsByName("Vegetables")).thenReturn(true);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(priceOverrideRepository.findByProductIdAndCountryCode(any(), any()))
                .thenReturn(Optional.empty());
        when(priceOverrideRepository.findByProductIdIn(any())).thenReturn(List.of());
        when(priceOverrideRepository.save(any(ProductPriceOverride.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(pricingService.resolveLoaded(any(Product.class), anyString(), any()))
                .thenReturn(new PricingService.ResolvedPrice(new BigDecimal("15.00"), null));
        when(pricingService.rateFor("IN")).thenReturn(new BigDecimal("62"));
    }

    /** Exactly what the sub-admin price drawer sends — no imageUrl/description/discount. */
    private UpdateProductRequest priceOnlyRequest() {
        return new UpdateProductRequest(
                "Ridge Gourd",
                "Vegetables",
                null, // description omitted
                new BigDecimal("45.00"),
                null,
                "per pc",
                null, // imageUrl omitted — the bug wiped the image here
                null, // discount omitted
                null, // badges omitted
                true,
                "IN");
    }

    @Test
    void priceOnlyUpdate_withoutImageUrl_keepsExistingImage() {
        service.update(productId, priceOnlyRequest());

        assertEquals(
                "https://res.cloudinary.com/eqjshlur/image/upload/v1790001287/vpnexues/Ridge%20Gourd.jpg",
                product.getImageUrl(),
                "price-only update must not wipe imageUrl");
    }

    @Test
    void priceOnlyUpdate_withoutDescriptionOrDiscount_keepsExistingValues() {
        service.update(productId, priceOnlyRequest());

        assertEquals("Farm-fresh ridge gourd", product.getDescription());
        assertEquals(21, product.getDiscountPct());
    }

    @Test
    void fullEdit_withNewImageUrl_replacesImage() {
        UpdateProductRequest req = new UpdateProductRequest(
                "Ridge Gourd",
                "Vegetables",
                "New description",
                new BigDecimal("45.00"),
                null,
                "per pc",
                "https://res.cloudinary.com/eqjshlur/image/upload/v999/new.jpg",
                10,
                List.of("Organic"),
                true,
                "IN");
        service.update(productId, req);

        assertEquals("https://res.cloudinary.com/eqjshlur/image/upload/v999/new.jpg", product.getImageUrl());
        assertEquals("New description", product.getDescription());
        assertEquals(10, product.getDiscountPct());
    }

    @Test
    void priceOnlyUpdate_withCountryCode_writesRegionOverride_notBasePrice() {
        service.update(productId, priceOnlyRequest());

        // SGD base untouched — only the IN override carries the new price
        assertEquals(new BigDecimal("15.00"), product.getBasePrice());

        ArgumentCaptor<ProductPriceOverride> captor = ArgumentCaptor.forClass(ProductPriceOverride.class);
        verify(priceOverrideRepository).save(captor.capture());
        ProductPriceOverride saved = captor.getValue();
        assertEquals("IN", saved.getCountryCode());
        assertEquals(new BigDecimal("45.00"), saved.getPrice());
        // the override points at the same product whose image survived the price edit
        assertEquals(product.getImageUrl(), saved.getProduct().getImageUrl());
    }

    @Test
    void priceOnlyUpdate_neverPersistsProductWithNullImage() {
        // Guard-level proof: the save() the repository receives still carries the original URL.
        service.update(productId, priceOnlyRequest());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals(product.getImageUrl(), captor.getValue().getImageUrl());
    }
}
