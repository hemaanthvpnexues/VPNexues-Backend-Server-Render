package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.AdminProductDto;
import com.vpnexues.svc.dto.CreateProductRequest;
import com.vpnexues.svc.dto.UpdateProductRequest;
import com.vpnexues.svc.entity.Product;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.CategoryRepository;
import com.vpnexues.svc.repository.ProductRepository;
import com.vpnexues.svc.util.Slugify;
import java.util.List;
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
        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public AdminProductDto get(UUID id) {
        return toDto(getEntity(id));
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

        Product product = new Product();
        product.setName(req.name());
        product.setSlug(slug);
        product.setCategory(req.category());
        product.setDescription(req.description());
        product.setBasePrice(req.basePrice());
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
        return toDto(saved);
    }

    public AdminProductDto update(UUID id, UpdateProductRequest req) {
        requireKnownCategory(req.category());
        Product product = getEntity(id);

        product.setName(req.name());
        product.setCategory(req.category());
        product.setDescription(req.description());
        product.setBasePrice(req.basePrice());
        product.setOldPrice(req.oldPrice());
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
        return toDto(productRepository.save(product));
    }

    /** Soft delete — orders reference products, so a hard delete would violate the FK. */
    public void deactivate(UUID id) {
        Product product = getEntity(id);
        product.setActive(false);
        productRepository.save(product);
    }

    private void requireKnownCategory(String category) {
        if (!categoryRepository.existsByName(category)) {
            throw new BadRequestException("Unknown category: " + category);
        }
    }

    private Product getEntity(UUID id) {
        return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }

    private AdminProductDto toDto(Product p) {
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
                p.getCreatedAt());
    }
}
