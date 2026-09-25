package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.BoxSummaryDto;
import com.vpnexues.svc.dto.CartDto;
import com.vpnexues.svc.dto.CartItemDto;
import com.vpnexues.svc.entity.Cart;
import com.vpnexues.svc.entity.CartItem;
import com.vpnexues.svc.entity.Product;
import com.vpnexues.svc.entity.User;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.CartItemRepository;
import com.vpnexues.svc.repository.CartRepository;
import com.vpnexues.svc.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private static final Set<String> VALID_BOX_TYPES = Set.of("SMALL_BOX", "BIG_BOX");
    private static final BigDecimal BIG_BOX_WEIGHT_STEP = new BigDecimal("0.5");

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductService productService;
    private final PricingService pricingService;

    public Cart getOrCreateCart(CartOwner owner) {
        if (owner.isUser()) {
            return cartRepository.findByUserId(owner.userId()).orElseGet(() -> {
                User user = userRepository
                        .findById(owner.userId())
                        .orElseThrow(() -> new NotFoundException("User not found: " + owner.userId()));
                Cart cart = new Cart();
                cart.setUser(user);
                return cartRepository.save(cart);
            });
        }
        return cartRepository.findByGuestToken(owner.guestToken()).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setGuestToken(owner.guestToken());
            return cartRepository.save(cart);
        });
    }

    public CartDto getCart(CartOwner owner, String countryCode) {
        return toDto(getOrCreateCart(owner));
    }

    public CartDto addItem(CartOwner owner, UUID productId, int qty, String countryCode) {
        Cart cart = getOrCreateCart(owner);
        Product product = productService.getEntityById(productId);
        PricingService.ResolvedPrice price = pricingService.resolve(product, countryCode);

        CartItem existing = cart.getItems().stream()
                .filter(i -> product.equals(i.getProduct()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setQty(existing.getQty() + qty);
            existing.setUnitPriceSnapshot(price.price());
            existing.setOldPriceSnapshot(price.oldPrice());
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQty(qty);
            item.setUnitPriceSnapshot(price.price());
            item.setOldPriceSnapshot(price.oldPrice());
            cart.getItems().add(item);
        }
        return toDto(cartRepository.save(cart));
    }

    /**
     * Upserts a Small/Big Box cart line, keyed by (cart, product, boxType) — sets the given qty/weightKg
     * as the new absolute value (not an increment), matching the box-builder page's "this product is now
     * selected at N units / X kg" interaction model. A non-positive qty/weightKg removes the line.
     */
    public CartDto setBoxItem(
            CartOwner owner, UUID productId, String boxType, Integer qty, BigDecimal weightKg, String countryCode) {
        if (!VALID_BOX_TYPES.contains(boxType)) {
            throw new BadRequestException("Invalid boxType: " + boxType);
        }
        Cart cart = getOrCreateCart(owner);
        Product product = productService.getEntityById(productId);
        PricingService.ResolvedPrice price = pricingService.resolve(product, countryCode);

        CartItem existing = cart.getItems().stream()
                .filter(i -> boxType.equals(i.getBoxType()) && product.equals(i.getProduct()))
                .findFirst()
                .orElse(null);

        if ("BIG_BOX".equals(boxType)) {
            if (weightKg == null || weightKg.remainder(BIG_BOX_WEIGHT_STEP).compareTo(BigDecimal.ZERO) != 0) {
                throw new BadRequestException("Big Box weightKg must be a positive multiple of 0.5");
            }
            if (weightKg.compareTo(BigDecimal.ZERO) <= 0) {
                if (existing != null) cart.getItems().remove(existing);
                return toDto(cartRepository.save(cart));
            }
            if (existing != null) {
                existing.setWeightKg(weightKg);
                existing.setUnitPriceSnapshot(price.price());
                existing.setOldPriceSnapshot(price.oldPrice());
            } else {
                CartItem item = new CartItem();
                item.setCart(cart);
                item.setProduct(product);
                item.setBoxType(boxType);
                item.setQty(1);
                item.setWeightKg(weightKg);
                item.setUnitPriceSnapshot(price.price());
                item.setOldPriceSnapshot(price.oldPrice());
                cart.getItems().add(item);
            }
        } else {
            int newQty = qty != null ? qty : 0;
            if (newQty <= 0) {
                if (existing != null) cart.getItems().remove(existing);
                return toDto(cartRepository.save(cart));
            }
            if (existing != null) {
                existing.setQty(newQty);
                existing.setUnitPriceSnapshot(price.price());
                existing.setOldPriceSnapshot(price.oldPrice());
            } else {
                CartItem item = new CartItem();
                item.setCart(cart);
                item.setProduct(product);
                item.setBoxType(boxType);
                item.setQty(newQty);
                item.setUnitPriceSnapshot(price.price());
                item.setOldPriceSnapshot(price.oldPrice());
                cart.getItems().add(item);
            }
        }
        return toDto(cartRepository.save(cart));
    }

    public CartDto updateItemQty(CartOwner owner, UUID itemId, int qty, BigDecimal weightKg) {
        Cart cart = getOrCreateCart(owner);
        CartItem item = cartItemRepository
                .findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new NotFoundException("Cart item not found: " + itemId));
        item.setQty(qty);
        if ("BIG_BOX".equals(item.getBoxType()) && weightKg != null) {
            if (weightKg.remainder(BIG_BOX_WEIGHT_STEP).compareTo(BigDecimal.ZERO) != 0
                    || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Big Box weightKg must be a positive multiple of 0.5");
            }
            item.setWeightKg(weightKg);
        }
        return toDto(cartRepository.save(cart));
    }

    public CartDto removeItem(CartOwner owner, UUID itemId) {
        Cart cart = getOrCreateCart(owner);
        CartItem item = cartItemRepository
                .findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new NotFoundException("Cart item not found: " + itemId));
        cart.getItems().remove(item);
        return toDto(cartRepository.save(cart));
    }

    public CartDto clear(CartOwner owner) {
        Cart cart = getOrCreateCart(owner);
        cart.getItems().clear();
        return toDto(cartRepository.save(cart));
    }

    /** Folds a guest cart's items into the now-authenticated user's cart on login, then discards the guest cart. */
    public void mergeGuestCartIntoUser(String guestToken, UUID userId) {
        if (guestToken == null) {
            return;
        }
        cartRepository.findByGuestToken(guestToken).ifPresent(guestCart -> {
            Cart userCart = getOrCreateCart(CartOwner.ofUser(userId));
            for (CartItem guestItem : guestCart.getItems()) {
                CartItem existing = userCart.getItems().stream()
                        .filter(i -> guestItem.getProduct() != null
                                && guestItem.getProduct().equals(i.getProduct())
                                && java.util.Objects.equals(guestItem.getBoxType(), i.getBoxType()))
                        .findFirst()
                        .orElse(null);
                if (existing != null) {
                    existing.setQty(existing.getQty() + guestItem.getQty());
                    if (guestItem.getWeightKg() != null) {
                        BigDecimal base = existing.getWeightKg() != null ? existing.getWeightKg() : BigDecimal.ZERO;
                        existing.setWeightKg(base.add(guestItem.getWeightKg()));
                    }
                } else {
                    CartItem moved = new CartItem();
                    moved.setCart(userCart);
                    moved.setProduct(guestItem.getProduct());
                    moved.setBoxType(guestItem.getBoxType());
                    moved.setQty(guestItem.getQty());
                    moved.setWeightKg(guestItem.getWeightKg());
                    moved.setUnitPriceSnapshot(guestItem.getUnitPriceSnapshot());
                    moved.setOldPriceSnapshot(guestItem.getOldPriceSnapshot());
                    userCart.getItems().add(moved);
                }
            }
            cartRepository.save(userCart);
            cartRepository.delete(guestCart);
        });
    }

    private CartDto toDto(Cart cart) {
        Map<String, List<CartItem>> grouped = pricingService.groupByBoxType(cart.getItems());
        Map<UUID, BigDecimal> lineTotalsByItemId = new HashMap<>();
        List<BoxSummaryDto> boxSummaries = new ArrayList<>();

        // Regular (non-box) items: today's plain unitPrice * qty math.
        for (CartItem item : grouped.getOrDefault("", List.of())) {
            lineTotalsByItemId.put(item.getId(), item.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(item.getQty())));
        }
        List<CartItem> smallBoxItems = grouped.get("SMALL_BOX");
        if (smallBoxItems != null && !smallBoxItems.isEmpty()) {
            var result = pricingService.computeSmallBoxPricing(smallBoxItems);
            result.lines().forEach(line -> lineTotalsByItemId.put(line.item().getId(), line.lineTotal()));
            boxSummaries.add(result.summary());
        }
        List<CartItem> bigBoxItems = grouped.get("BIG_BOX");
        if (bigBoxItems != null && !bigBoxItems.isEmpty()) {
            var result = pricingService.computeBigBoxPricing(bigBoxItems);
            result.lines().forEach(line -> lineTotalsByItemId.put(line.item().getId(), line.lineTotal()));
            boxSummaries.add(result.summary());
        }

        var items = cart.getItems().stream()
                .map(i -> {
                    Product p = i.getProduct();
                    BigDecimal lineTotal = lineTotalsByItemId.get(i.getId());
                    return new CartItemDto(
                            i.getId(),
                            p != null ? p.getId() : null,
                            p != null ? p.getName() : "Box item",
                            p != null ? p.getImageUrl() : null,
                            p != null ? p.getUnit() : null,
                            i.getBoxType(),
                            i.getQty(),
                            i.getWeightKg(),
                            i.getUnitPriceSnapshot(),
                            i.getOldPriceSnapshot(),
                            lineTotal);
                })
                .toList();

        BigDecimal itemTotal = items.stream().map(CartItemDto::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartDto(cart.getId(), items, itemTotal, boxSummaries);
    }
}
