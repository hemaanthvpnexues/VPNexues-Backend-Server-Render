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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.EntityManager;
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
    private final EntityManager entityManager;

    public Cart getOrCreateCart(CartOwner owner) {
        // FAST PATH: cart already exists -> read it with NO lock. The advisory lock used to be taken on every
        // call, including plain GET /api/cart, which serialized all concurrent reads for the same cart. Each
        // transaction holds it for ~1-3 s of database round trips, so the browser's parallel cart requests
        // queued up and later ones blew past the 10 s client timeout -> "Something went wrong".
        if (owner.isUser()) {
            Optional<Cart> existing = cartRepository.findByUserId(owner.userId());
            if (existing.isPresent()) {
                return existing.get();
            }
        } else {
            Optional<Cart> existing = cartRepository.findByGuestToken(owner.guestToken());
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        // CREATE PATH ONLY: serialize cart CREATION for this owner. The cart row cannot be row-locked yet (it
        // may not exist), so N simultaneous first-ever requests used to all INSERT the same user_id /
        // guest_token and all but one died on unique constraint "carts_*_key" -> 500 -> the item silently
        // vanished on the client. The losers wait here, then see the winner's row and reuse it. Re-entrant
        // within the same transaction; released automatically on commit/rollback.
        lockOwner(owner);
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

    /**
     * Takes a transaction-scoped Postgres advisory lock keyed on the cart owner (user id or guest token).
     * Must be called inside an active transaction - the lock is released when that transaction ends.
     */
    private void lockOwner(CartOwner owner) {
        String key = owner.isUser() ? "cart:user:" + owner.userId() : "cart:guest:" + owner.guestToken();
        // 32-bit hash widened to bigint; only needs to be stable, not collision-free (a collision just
        // serializes two unrelated carts for the duration of one transaction).
        entityManager
                .createNativeQuery("select pg_advisory_xact_lock(cast(:k as bigint))")
                .setParameter("k", (long) key.hashCode())
                .getSingleResult();
    }

    public CartDto getCart(CartOwner owner, String countryCode) {
        return toDto(getOrCreateCart(owner));
    }

    /**
     * Loads the owner's cart with a row-level {@code SELECT ... FOR UPDATE} lock held for the rest of this
     * transaction, so concurrent mutations serialize instead of overwriting each other.
     *
     * <p>Brand-new carts need no lock - nothing else can be reading them yet, and the unique constraint on
     * user_id / guest_token is what protects the create path.
     */
    private Cart lockCart(CartOwner owner) {
        if (owner.isUser()) {
            return cartRepository
                    .findByUserIdForUpdate(owner.userId())
                    .orElseGet(() -> getOrCreateCart(owner));
        }
        return cartRepository
                .findByGuestTokenForUpdate(owner.guestToken())
                .orElseGet(() -> getOrCreateCart(owner));
    }

    public CartDto addItem(CartOwner owner, UUID productId, int qty, String countryCode) {
        Cart cart = lockCart(owner);
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
        Cart cart = lockCart(owner);
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
        Cart cart = lockCart(owner);
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
        Cart cart = lockCart(owner);
        CartItem item = cartItemRepository
                .findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new NotFoundException("Cart item not found: " + itemId));
        cart.getItems().remove(item);
        return toDto(cartRepository.save(cart));
    }

    public CartDto clear(CartOwner owner) {
        Cart cart = lockCart(owner);
        cart.getItems().clear();
        return toDto(cartRepository.save(cart));
    }

    /** Folds a guest cart's items into the now-authenticated user's cart on login, then discards the guest cart. */
    public void mergeGuestCartIntoUser(String guestToken, UUID userId) {
        if (guestToken == null) {
            return;
        }
        cartRepository.findByGuestTokenForUpdate(guestToken).ifPresent(guestCart -> {
            Cart userCart = lockCart(CartOwner.ofUser(userId));
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
