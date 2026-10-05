package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.AdminProductDto;
import com.vpnexues.svc.dto.CreateProductRequest;
import com.vpnexues.svc.dto.UpdateProductRequest;
import com.vpnexues.svc.entity.Product;
import com.vpnexues.svc.entity.ProductPriceOverride;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.CategoryRepository;
import com.vpnexues.svc.repository.ProductPriceOverrideRepository;
import com.vpnexues.svc.repository.ProductRepository;
import com.vpnexues.svc.util.Slugify;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryItemService inventoryItemService;
    private final ProductPriceOverrideRepository priceOverrideRepository;
    private final PricingService pricingService;

    @Transactional(readOnly = true)
    public Page<AdminProductDto> list(String category, String search, Pageable pageable) {
        Page<Product> page;
        if (StringUtils.hasText(search)) {
            page = productRepository.findByNameContainingIgnoreCase(search, pageable);
        } else if (StringUtils.hasText(category)) {
            page = productRepository.findByCategory(category, pageable);
        } else {
            page = productRepository.findAll(pageable);
        }
        Map<UUID, Map<String, ProductPriceOverride>> overrides =
                loadOverrides(page.getContent().stream().map(Product::getId).toList());
        return page.map(p -> toDto(p, overrides));
    }

    @Transactional(readOnly = true)
    public AdminProductDto get(UUID id) {
        Product p = getEntity(id);
        return toDto(p, loadOverrides(List.of(p.getId())));
    }

    public AdminProductDto create(CreateProductRequest req) {
        requireKnownCategory(req.category());
        if (productRepository.existsBySku(req.sku())) {
            throw new BadRequestException("A product with SKU '" + req.sku() + "' already exists");
        }
        String slug = StringUtils.hasText(req.slug()) ? Slugify.slugify(req.slug()) : Slugify.slugify(req.name());
        if (productRepository.existsBySlug(slug)) {
            throw new BadRequestException("A product with slug '" + slug + "' already exists");
        }

        String region = StringUtils.hasText(req.countryCode()) ? req.countryCode().trim().toUpperCase() : null;
        Product product = new Product();
        product.setName(req.name());
        product.setSlug(slug);
        product.setCategory(req.category());
        product.setDescription(req.description());
        // Region-aware create: the entered price belongs to the acting market, so the SGD base
        // is derived (price / rate) and an override pins the market price exactly as entered.
        product.setBasePrice(region != null
                ? req.basePrice().divide(pricingService.rateFor(region), 2, java.math.RoundingMode.HALF_UP)
                : req.basePrice());
        product.setOldPrice(req.oldPrice());
        product.setUnit(req.unit());
        product.setSku(req.sku());
        product.setImageUrl(req.imageUrl());
        product.setDiscountPct(req.discountPct());
        if (req.badges() != null) {
            product.setBadges(req.badges());
        }
        Product saved = productRepository.save(product);
        inventoryItemService.createDefaultFor(saved);
        if (region != null) {
            upsertRegionPrice(saved, region, req.basePrice(), req.oldPrice());
        }
        return toDto(saved, loadOverrides(List.of(saved.getId())));
    }

    public AdminProductDto update(UUID id, UpdateProductRequest req) {
        requireKnownCategory(req.category());
        Product product = getEntity(id);

        String region = StringUtils.hasText(req.countryCode()) ? req.countryCode().trim().toUpperCase() : null;
        product.setName(req.name());
        product.setCategory(req.category());
        product.setDescription(req.description());
        // With countryCode the price edits target that market's override (what the shop reads
        // for that region); the SGD base stays untouched so other regions don't shift.
        if (region == null) {
            product.setBasePrice(req.basePrice());
            product.setOldPrice(req.oldPrice());
        }
        product.setUnit(req.unit());
        product.setImageUrl(req.imageUrl());
        product.setDiscountPct(req.discountPct());
        // Mutate the existing (mutable) collection in place — replacing it with List.of()
        // made Hibernate's flush throw UnsupportedOperationException (500 on every
        // update that omitted badges, i.e. the admin's price edits and form saves).
        if (req.badges() != null) {
            product.getBadges().clear();
            product.getBadges().addAll(req.badges());
        }
        product.setActive(req.active());
        Product saved = productRepository.save(product);
        if (region != null) {
            upsertRegionPrice(saved, region, req.basePrice(), req.oldPrice());
        }
        return toDto(saved, loadOverrides(List.of(saved.getId())));
    }

    /** Soft delete — orders reference products, so a hard delete would violate the FK. */
    public void deactivate(UUID id) {
        Product product = getEntity(id);
        product.setActive(false);
        productRepository.save(product);
    }

    /** Create-or-update the market override so the shop for that region shows the new price. */
    private void upsertRegionPrice(Product product, String region, BigDecimal price, BigDecimal requestedOld) {
        ProductPriceOverride o = priceOverrideRepository
                .findByProductIdAndCountryCode(product.getId(), region)
                .orElse(null);
        BigDecimal current = pricingService.resolveLoaded(product, region, o).price();
        if (o == null) {
            o = new ProductPriceOverride();
            o.setProduct(product);
            o.setCountryCode(region);
        }
        o.setPrice(price);
        // Explicit old price wins; otherwise keep the previous effective price as the strike-through
        // (price-drop history), unless the price didn't actually change.
        o.setOldPrice(requestedOld != null
                ? requestedOld
                : (current.compareTo(price) != 0 ? current : null));
        priceOverrideRepository.save(o);
    }

    /** One query loads every override for the page — grouped by product, then by market. */
    private Map<UUID, Map<String, ProductPriceOverride>> loadOverrides(List<UUID> productIds) {
        Map<UUID, Map<String, ProductPriceOverride>> byProduct = new HashMap<>();
        if (productIds.isEmpty()) {
            return byProduct;
        }
        for (ProductPriceOverride o : priceOverrideRepository.findByProductIdIn(productIds)) {
            byProduct.computeIfAbsent(o.getProduct().getId(), k -> new HashMap<>())
                    .put(o.getCountryCode().toUpperCase(), o);
        }
        return byProduct;
    }

    private void requireKnownCategory(String category) {
        if (!categoryRepository.existsByName(category)) {
            throw new BadRequestException("Unknown category: " + category);
        }
    }

    private Product getEntity(UUID id) {
        return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }

    private AdminProductDto toDto(Product p, Map<UUID, Map<String, ProductPriceOverride>> overrides) {
        Map<String, ProductPriceOverride> productOverrides = overrides.getOrDefault(p.getId(), Map.of());
        Map<String, AdminProductDto.RegionalPrice> prices = new LinkedHashMap<>();
        for (String region : PricingService.PRICED_REGIONS) {
            PricingService.ResolvedPrice r =
                    pricingService.resolveLoaded(p, region, productOverrides.get(region));
            prices.put(region, new AdminProductDto.RegionalPrice(r.price(), r.oldPrice()));
        }
        // Markets outside the four priced regions that have an explicit override (LK/MY) —
        // the shop reads those directly, so the admin must show them too.
        productOverrides.forEach((region, o) -> prices.putIfAbsent(
                region, new AdminProductDto.RegionalPrice(o.getPrice(), o.getOldPrice())));
        return new AdminProductDto(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getCategory(),
                p.getDescription(),
                p.getBasePrice(),
                p.getOldPrice(),
                p.getUnit(),
                p.getSku(),
                p.getImageUrl(),
                p.getDiscountPct(),
                List.copyOf(p.getBadges()),
                p.isActive(),
                p.getCreatedAt(),
                prices);
    }
}
