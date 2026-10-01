package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.ProductDto;
import com.vpnexues.svc.entity.ProductPriceOverride;
import com.vpnexues.svc.entity.Product;
import com.vpnexues.svc.repository.ProductPriceOverrideRepository;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductPriceOverrideRepository priceOverrideRepository;
    private final PricingService pricingService;

    public Page<ProductDto> list(String category, String search, String countryCode, Pageable pageable) {
        String cc = countryCode != null ? countryCode.toUpperCase() : "IN";
        Page<Product> page;
        if (StringUtils.hasText(search)) {
            page = switch (cc) {
                case "SG" -> productRepository.findByActiveTrueAndNameContainingIgnoreCaseOrderBySalesCountSgDesc(search, pageable);
                case "US" -> productRepository.findByActiveTrueAndNameContainingIgnoreCaseOrderBySalesCountUsDesc(search, pageable);
                case "AE" -> productRepository.findByActiveTrueAndNameContainingIgnoreCaseOrderBySalesCountAeDesc(search, pageable);
                default -> productRepository.findByActiveTrueAndNameContainingIgnoreCaseOrderBySalesCountDesc(search, pageable);
            };
        } else if (StringUtils.hasText(category)) {
            page = switch (cc) {
                case "SG" -> productRepository.findByActiveTrueAndCategoryOrderBySalesCountSgDesc(category, pageable);
                case "US" -> productRepository.findByActiveTrueAndCategoryOrderBySalesCountUsDesc(category, pageable);
                case "AE" -> productRepository.findByActiveTrueAndCategoryOrderBySalesCountAeDesc(category, pageable);
                default -> productRepository.findByActiveTrueAndCategoryOrderBySalesCountDesc(category, pageable);
            };
        } else {
            page = switch (cc) {
                case "SG" -> productRepository.findByActiveTrueOrderBySalesCountSgDesc(pageable);
                case "US" -> productRepository.findByActiveTrueOrderBySalesCountUsDesc(pageable);
                case "AE" -> productRepository.findByActiveTrueOrderBySalesCountAeDesc(pageable);
                default -> productRepository.findByActiveTrueOrderBySalesCountDesc(pageable);
            };
        }
        List<Product> products = page.getContent();
        List<UUID> ids = products.stream().map(Product::getId).toList();
        if (!ids.isEmpty()) {
            productRepository.findWithBadgesByIdIn(ids);
            List<ProductPriceOverride> overrides = priceOverrideRepository.findByCountryCodeAndProductIdIn(cc, ids);
            java.util.Map<UUID, ProductPriceOverride> overrideMap = overrides.stream()
                    .collect(java.util.stream.Collectors.toMap(o -> o.getProduct().getId(), o -> o));
            return new org.springframework.data.domain.PageImpl<>(
                    products.stream().map(p -> toDto(p, countryCode, overrideMap.get(p.getId())))
                            .toList(), pageable, page.getTotalElements());
        }
        return page.map(p -> toDto(p, countryCode, null));
    }

    public ProductDto getBySlug(String slug, String countryCode) {
        Product product = productRepository
                .findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new NotFoundException("Product not found: " + slug));
        return toDto(product, countryCode, null);
    }

    Product getEntityById(java.util.UUID id) {
        return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }

    ProductDto toDto(Product product, String countryCode, ProductPriceOverride override) {
        PricingService.ResolvedPrice resolved = override != null
                ? pricingService.resolve(product, countryCode, override)
                : pricingService.resolve(product, countryCode);
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getCategory(),
                product.getDescription(),
                resolved.price(),
                resolved.oldPrice(),
                product.getUnit(),
                product.getSku(),
                product.getImageUrl(),
                product.getDiscountPct(),
                // Force the lazy @ElementCollection to load now, while the
                // transaction/session is still open — open-in-view is disabled,
                // so leaving this as a lazy reference fails during JSON
                // serialization later with "no session".
                List.copyOf(product.getBadges()));
    }
}
