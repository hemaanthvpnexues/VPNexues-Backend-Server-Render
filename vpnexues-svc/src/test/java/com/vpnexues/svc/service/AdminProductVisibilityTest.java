package com.vpnexues.svc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vpnexues.svc.dto.AdminProductDto;
import com.vpnexues.svc.entity.AdminRole;
import com.vpnexues.svc.entity.AdminUser;
import com.vpnexues.svc.entity.Product;
import com.vpnexues.svc.entity.ProductPriceOverride;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.AdminUserRepository;
import com.vpnexues.svc.repository.CategoryRepository;
import com.vpnexues.svc.repository.ProductPriceOverrideRepository;
import com.vpnexues.svc.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

/**
 * Tests for the dedicated hide/show grant behind {@code PUT /api/admin/products/{id}/visibility}.
 *
 * <p>Two properties are worth protecting here. First, a sub-admin granted only the new {@code hide}
 * grant can toggle visibility without a full {@code edit}. Second, and more importantly, the
 * operation writes <b>only</b> the {@code active} flag — the endpoint exists separately from
 * {@code PUT /{id}} precisely so that hiding a product can never move a price.
 */
class AdminProductVisibilityTest {

    private static final BigDecimal BASE_PRICE = new BigDecimal("60.00");

    private ProductRepository productRepository;
    private ProductPriceOverrideRepository priceOverrideRepository;
    private AdminUserRepository adminUserRepository;
    private PricingService pricingService;

    private AdminAccessService accessService;
    private AdminProductService service;

    private UUID adminId;
    private Product product;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        priceOverrideRepository = mock(ProductPriceOverrideRepository.class);
        adminUserRepository = mock(AdminUserRepository.class);
        pricingService = mock(PricingService.class);

        accessService = new AdminAccessService(adminUserRepository);
        service =
                new AdminProductService(
                        productRepository,
                        mock(CategoryRepository.class),
                        mock(InventoryItemService.class),
                        priceOverrideRepository,
                        pricingService);

        // toDto() resolves every region's effective price; honour a loaded override, else base.
        when(pricingService.resolveLoaded(any(), anyString(), any()))
                .thenAnswer(
                        (inv) -> {
                            ProductPriceOverride o = inv.getArgument(2);
                            return new PricingService.ResolvedPrice(
                                    o != null ? o.getPrice() : BASE_PRICE, null);
                        });

        adminId = UUID.randomUUID();
        product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Alphonso Mangoes");
        product.setCategory("fruits");
        product.setSku("MNG-ALP");
        product.setUnit("per kg");
        product.setBasePrice(BASE_PRICE);
        product.setActive(true);

        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer((inv) -> inv.getArgument(0));
        when(priceOverrideRepository.findByProductIdIn(List.of(product.getId())))
                .thenReturn(List.of());
    }

    /** Stub the acting sub-admin with the given module and permission CSVs. */
    private AdminUser subAdmin(String moduleCsv, String permissionCsv) {
        AdminUser admin = new AdminUser();
        admin.setId(adminId);
        admin.setRole(AdminRole.SUB_ADMIN);
        admin.setModules(moduleCsv);
        admin.setPermissions(permissionCsv);
        when(adminUserRepository.findById(adminId)).thenReturn(Optional.of(admin));
        return admin;
    }

    // ---------------------------------------------------------------- the grant itself

    @Test
    @DisplayName("'hide' alone is enough to take a product off the shop")
    void hideGrantAloneHidesTheProduct() {
        subAdmin("products", "view,hide");

        AdminProductDto dto = service.setVisibility(product.getId(), false);

        assertFalse(dto.active());
        assertFalse(product.isActive());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("'hide' alone is enough to put a product back on the shop")
    void hideGrantAloneShowsTheProduct() {
        subAdmin("products", "view,hide");
        product.setActive(false);

        AdminProductDto dto = service.setVisibility(product.getId(), true);

        assertTrue(dto.active());
        assertTrue(product.isActive());
    }

    @Test
    @DisplayName("A full 'edit' grant with no 'hide' still works, so existing accounts keep the ability")
    void editGrantStillAllowsVisibility() {
        subAdmin("products", "view,edit");

        service.setVisibility(product.getId(), false);

        assertFalse(product.isActive());
    }

    @Test
    @DisplayName("A legacy account with a blank module column is unrestricted and may hide anything")
    void blankModulesRemainUnrestricted() {
        subAdmin("", "");

        service.setVisibility(product.getId(), false);

        assertFalse(product.isActive());
    }

    @Test
    @DisplayName("Permission CSVs are trimmed, so ' view , hide ' still grants hide")
    void permissionCsvIsTrimmed() {
        subAdmin("products", " view , hide ");

        service.setVisibility(product.getId(), false);

        assertFalse(product.isActive());
    }

    @Test
    @DisplayName("Hiding an unknown product is a 404, not a permission error")
    void unknownProductIsNotFound() {
        subAdmin("products", "view,hide");
        UUID missing = UUID.randomUUID();
        when(productRepository.findById(missing)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.setVisibility(missing, false));
    }

    /**
     * Authorisation is enforced by the controller, not this service (matching how every other
     * product mutation works), so the denial cases assert against
     * {@code requireAnyPermission} — the exact call the visibility endpoint makes.
     */

    @Test
    @DisplayName("A permission that merely contains the word hide is not a grant")
    void partialPermissionNameDoesNotGrant() {
        subAdmin("products", "view,hideeverything");

        assertThrows(
                AccessDeniedException.class,
                () ->
                        accessService.requireAnyPermission(
                                adminId,
                                AdminAccessService.M_PRODUCTS,
                                AdminAccessService.P_HIDE,
                                AdminAccessService.P_EDIT));
    }

    @Test
    @DisplayName("'hide' on a module the sub-admin was not granted is denied")
    void hideWithoutTheProductsModuleIsDenied() {
        subAdmin("orders", "view,hide");

        assertThrows(
                AccessDeniedException.class,
                () ->
                        accessService.requireAnyPermission(
                                adminId,
                                AdminAccessService.M_PRODUCTS,
                                AdminAccessService.P_HIDE,
                                AdminAccessService.P_EDIT));
    }

    @Test
    @DisplayName("Neither 'hide' nor 'edit' on products is denied, so the toggle never fires")
    void neitherHideNorEditIsDenied() {
        subAdmin("products", "view,export");

        assertThrows(
                AccessDeniedException.class,
                () ->
                        accessService.requireAnyPermission(
                                adminId,
                                AdminAccessService.M_PRODUCTS,
                                AdminAccessService.P_HIDE,
                                AdminAccessService.P_EDIT));
    }

    @Test
    @DisplayName("An unknown admin id is denied before anything else is considered")
    void unknownAdminIsDenied() {
        when(adminUserRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () ->
                        accessService.requireAnyPermission(
                                adminId, AdminAccessService.M_PRODUCTS, AdminAccessService.P_HIDE));
    }

    // ---------------------------------------------------------------- the price-safety guarantee

    @Test
    @DisplayName("A visibility change writes only `active` — name, category and base price are untouched")
    void visibilityWritesOnlyTheActiveFlag() {
        subAdmin("products", "view,edit,hide");

        service.setVisibility(product.getId(), false);

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(saved.capture());
        assertEquals(product.getId(), saved.getValue().getId());
        assertEquals("Alphonso Mangoes", saved.getValue().getName());
        assertEquals("fruits", saved.getValue().getCategory());
        assertEquals(0, BASE_PRICE.compareTo(saved.getValue().getBasePrice()));
    }

    @Test
    @DisplayName("The update path that writes region prices is never reached")
    void visibilityNeverTouchesRegionPrices() {
        subAdmin("products", "view,edit,hide");

        service.setVisibility(product.getId(), false);

        // loadOverrides() *reads* overrides to build the DTO; the write path is never entered.
        verify(priceOverrideRepository, never())
                .findByProductIdAndCountryCode(any(UUID.class), anyString());
        assertEquals(0, BASE_PRICE.compareTo(product.getBasePrice()));
    }

    @Test
    @DisplayName("A product with an existing region override keeps that override exactly")
    void existingRegionOverridesAreLeftAlone() {
        subAdmin("products", "view,edit,hide");
        ProductPriceOverride in = new ProductPriceOverride();
        in.setProduct(product);
        in.setCountryCode("IN");
        in.setPrice(new BigDecimal("99.00"));
        when(priceOverrideRepository.findByProductIdIn(List.of(product.getId())))
                .thenReturn(List.of(in));

        AdminProductDto dto = service.setVisibility(product.getId(), false);

        assertFalse(dto.active());
        assertEquals(0, new BigDecimal("99.00").compareTo(dto.prices().get("IN").price()));
        // The base row still carries the SGD base, not the Indian override.
        assertEquals(0, BASE_PRICE.compareTo(product.getBasePrice()));
    }

    @Test
    @DisplayName("Hidden products stay in the admin list — nothing deletes or reloads the row")
    void hidingIsNotADelete() {
        subAdmin("products", "view,hide");

        service.setVisibility(product.getId(), false);

        verify(productRepository, never()).delete(any(Product.class));
        assertFalse(product.isActive());
    }

    @Test
    @DisplayName("Toggling twice returns to the original state")
    void togglingTwiceIsIdempotent() {
        subAdmin("products", "view,hide");

        service.setVisibility(product.getId(), false);
        assertFalse(product.isActive());
        service.setVisibility(product.getId(), true);
        assertTrue(product.isActive());
    }

    // ---------------------------------------------------------------- requireAnyPermission

    @Test
    @DisplayName("requireAnyPermission passes as soon as one listed grant matches")
    void requireAnyPermissionAcceptsAnyListedGrant() {
        subAdmin("products", "view,export");

        accessService.requireAnyPermission(
                adminId,
                AdminAccessService.M_PRODUCTS,
                AdminAccessService.P_DELETE,
                AdminAccessService.P_EXPORT);
    }

    @Test
    @DisplayName("requireAnyPermission denies when none of the listed grants are present")
    void requireAnyPermissionDeniesWhenNoneMatch() {
        subAdmin("products", "view");

        assertThrows(
                AccessDeniedException.class,
                () ->
                        accessService.requireAnyPermission(
                                adminId,
                                AdminAccessService.M_PRODUCTS,
                                AdminAccessService.P_DELETE,
                                AdminAccessService.P_HIDE));
    }

    @Test
    @DisplayName("requireAnyPermission still enforces the module even when the grant is listed")
    void requireAnyPermissionEnforcesTheModule() {
        subAdmin("orders", "view,hide,export");

        assertThrows(
                AccessDeniedException.class,
                () ->
                        accessService.requireAnyPermission(
                                adminId,
                                AdminAccessService.M_PRODUCTS,
                                AdminAccessService.P_HIDE,
                                AdminAccessService.P_EDIT));
    }

    @Test
    @DisplayName("requireAnyPermission short-circuits for an unrestricted account")
    void requireAnyPermissionShortCircuitsForUnrestricted() {
        subAdmin("", "");

        accessService.requireAnyPermission(
                adminId, AdminAccessService.M_PRODUCTS, AdminAccessService.P_HIDE);
    }

    @Test
    @DisplayName("The denial message names the module that was checked")
    void denialMessageNamesTheModule() {
        subAdmin("products", "view");

        AccessDeniedException ex =
                assertThrows(
                        AccessDeniedException.class,
                        () ->
                                accessService.requireAnyPermission(
                                        adminId, AdminAccessService.M_PRODUCTS, AdminAccessService.P_HIDE));

        assertTrue(ex.getMessage().contains(AdminAccessService.M_PRODUCTS));
    }
}
