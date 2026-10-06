package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.BoxSummaryDto;
import com.vpnexues.svc.dto.CartDto;
import com.vpnexues.svc.dto.CheckoutRequest;
import com.vpnexues.svc.dto.CouponApplyResponse;
import com.vpnexues.svc.dto.OrderDto;
import com.vpnexues.svc.dto.OrderItemDto;
import com.vpnexues.svc.entity.Address;
import com.vpnexues.svc.entity.InventoryItem;
import com.vpnexues.svc.entity.Order;
import com.vpnexues.svc.entity.OrderChannel;
import com.vpnexues.svc.entity.OrderItem;
import com.vpnexues.svc.entity.OrderStatus;
import com.vpnexues.svc.entity.Payment;
import com.vpnexues.svc.entity.PaymentStatus;
import com.vpnexues.svc.entity.User;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.InventoryItemRepository;
import com.vpnexues.svc.repository.OrderRepository;
import com.vpnexues.svc.repository.PaymentRepository;
import com.vpnexues.svc.repository.ProductRepository;
import com.vpnexues.svc.repository.UserRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final BigDecimal NON_IN_DELIVERY_FEE = BigDecimal.valueOf(8);
    /** Customers may self-cancel only inside this window after placing the order. */
    private static final Duration CANCEL_WINDOW = Duration.ofHours(1);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final CartService cartService;
    private final CouponService couponService;
    private final AddressService addressService;

    public OrderDto checkout(UUID userId, String countryCode, CheckoutRequest req) {
        CartOwner owner = CartOwner.ofUser(userId);
        CartDto cart = cartService.getCart(owner, countryCode);
        if (cart.items().isEmpty()) {
            throw new BadRequestException("Cannot checkout an empty cart");
        }

        // Validate stock availability for all cart items
        List<UUID> productIds = cart.items().stream()
                .map(ci -> ci.productId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, Integer> availableByProduct = productIds.isEmpty()
                ? Map.of()
                : inventoryItemRepository.findAllByProductIdIn(productIds).stream()
                        .collect(Collectors.toMap(
                                inv -> inv.getProduct().getId(),
                                InventoryItem::getQuantity,
                                (a, b) -> a));
        for (var ci : cart.items()) {
            if (ci.productId() != null) {
                Integer available = availableByProduct.get(ci.productId());
                if (available == null || available < ci.qty()) {
                    throw new BadRequestException(
                            "Insufficient stock for " + ci.name() + " (available: "
                                    + (available == null ? "0" : String.valueOf(available))
                                    + ", requested: " + ci.qty() + ")");
                }
            }
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        UUID addressUuid;
        try {
            addressUuid = UUID.fromString(req.addressId());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid address ID format: " + req.addressId());
        }
        Address address = addressService.getEntityForUser(addressUuid, userId);

        BigDecimal itemTotal = cart.itemTotal();
        BigDecimal discount = BigDecimal.ZERO;
        String appliedCouponCode = null;
        if (req.couponCode() != null && !req.couponCode().isBlank()) {
            CouponApplyResponse couponResult = couponService.apply(req.couponCode(), itemTotal, userId);
            if (!couponResult.valid()) {
                throw new BadRequestException(couponResult.message());
            }
            discount = couponResult.discountAmount();
            appliedCouponCode = couponResult.code();
        }

        BigDecimal boxDiscount = cart.boxSummaries().stream()
                .filter(s -> "BIG_BOX".equals(s.boxType()))
                .map(BoxSummaryDto::premiumOrDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrderChannel channel = cart.items().stream().anyMatch(i -> "BIG_BOX".equals(i.boxType()))
                ? OrderChannel.BIG_BOX
                : cart.items().stream().anyMatch(i -> "SMALL_BOX".equals(i.boxType()))
                        ? OrderChannel.SMALL_BOX
                        : OrderChannel.SHOP;

        BigDecimal deliveryFee = "IN".equalsIgnoreCase(countryCode) ? BigDecimal.ZERO : NON_IN_DELIVERY_FEE;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal tip = req.tip() != null ? req.tip() : BigDecimal.ZERO;
        BigDecimal grandTotal =
                itemTotal.subtract(discount).subtract(boxDiscount).add(deliveryFee).add(tax).add(tip);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            grandTotal = BigDecimal.ZERO;
        }

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setAddress(address);
        order.setStatus(OrderStatus.PLACED);
        order.setChannel(channel);
        order.setItemTotal(itemTotal);
        order.setDiscount(discount);
        order.setBoxDiscount(boxDiscount);
        order.setDeliveryFee(deliveryFee);
        order.setTax(tax);
        order.setTip(tip);
        order.setGrandTotal(grandTotal);
        order.setPaymentMethod(req.paymentMethod());
        order.setCouponCode(appliedCouponCode);
        order.setCountryCode(countryCode);

        List<OrderItem> orderItems = cart.items().stream().map(ci -> {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setName(ci.name());
            oi.setImageUrl(ci.imageUrl());
            oi.setBoxType(ci.boxType());
            oi.setQty(ci.qty());
            oi.setWeightKg(ci.weightKg());
            oi.setUnitPrice(ci.unitPrice());
            oi.setLineTotal(ci.lineTotal());
            return oi;
        }).toList();
        order.getItems().addAll(orderItems);

        Order saved = orderRepository.save(order);

        // Decrement inventory for each cart item
        for (var ci : cart.items()) {
            if (ci.productId() != null) {
                inventoryItemRepository.findByProductId(ci.productId()).ifPresent(inv -> {
                    inv.setQuantity(Math.max(0, inv.getQuantity() - ci.qty()));
                    inventoryItemRepository.save(inv);
                });
            }
        }

        // Realtime bestseller per country: increment global (IN) and country-specific column
        String cc = countryCode != null ? countryCode.toUpperCase() : "IN";
        for (var ci : cart.items()) {
            if (ci.productId() != null) {
                productRepository.findById(ci.productId()).ifPresent(p -> {
                    int inc = ci.qty() > 0 ? ci.qty() : 1;
                    p.setSalesCount(p.getSalesCount() + inc);
                    switch (cc) {
                        case "SG" -> p.setSalesCountSg(p.getSalesCountSg() + inc);
                        case "US" -> p.setSalesCountUs(p.getSalesCountUs() + inc);
                        case "AE" -> p.setSalesCountAe(p.getSalesCountAe() + inc);
                        default -> { /* IN already via salesCount */ }
                    }
                    productRepository.save(p);
                });
            }
        }

        // Payments are simulated for this build — see plan decisions. Marked paid immediately.
        Payment payment = new Payment();
        payment.setOrder(saved);
        payment.setAmount(grandTotal);
        payment.setMethod(req.paymentMethod());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);

        cartService.clear(owner);

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> listForUser(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDto getForUser(UUID orderId, UUID userId) {
        Order order = orderRepository
                .findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        return toDto(order);
    }

    /**
     * Customer self-service cancellation. Two guards mirror the order status state
     * machine: the current status must allow a transition to CANCELLED, and the
     * order must be younger than {@link #CANCEL_WINDOW}.
     */
    public OrderDto cancelForUser(UUID orderId, UUID userId) {
        Order order = orderRepository
                .findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            throw new BadRequestException("Order cannot be cancelled in status " + order.getStatus());
        }
        if (order.getCreatedAt().isBefore(Instant.now().minus(CANCEL_WINDOW))) {
            throw new BadRequestException("Orders can only be cancelled within 1 hour of placing them");
        }
        order.setStatus(OrderStatus.CANCELLED);
        return toDto(orderRepository.save(order));
    }

    private static final int MAX_ORDER_NUMBER_RETRIES = 10;

    private String generateOrderNumber() {
        for (int i = 0; i < MAX_ORDER_NUMBER_RETRIES; i++) {
            String candidate = "HB-" + (100000 + RANDOM.nextInt(900000));
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        // Fallback: timestamp-based to guarantee uniqueness
        return "HB-" + System.currentTimeMillis();
    }

    private OrderDto toDto(Order order) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(i -> new OrderItemDto(
                        i.getName(), i.getImageUrl(), i.getBoxType(), i.getQty(), i.getWeightKg(), i.getUnitPrice(), i.getLineTotal()))
                .toList();
        return new OrderDto(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getChannel().name(),
                order.getItemTotal(),
                order.getDiscount(),
                order.getBoxDiscount(),
                order.getDeliveryFee(),
                order.getTax(),
                order.getTip(),
                order.getGrandTotal(),
                order.getPaymentMethod(),
                order.getCouponCode(),
                items,
                order.getCreatedAt());
    }
}
