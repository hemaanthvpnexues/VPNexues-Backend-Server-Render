package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.CouponApplyResponse;
import com.vpnexues.svc.entity.Coupon;
import com.vpnexues.svc.entity.CouponType;
import com.vpnexues.svc.repository.CouponRepository;
import com.vpnexues.svc.repository.OrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    /** First-order-only codes: rejected when the customer already has any past order. */
    private static final String FIRST_ORDER_CODE = "FIRST200";

    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;

    /**
     * @param userId authenticated customer, or null for a guest preview (guests are
     *               re-checked at checkout once signed in)
     */
    public CouponApplyResponse apply(String code, BigDecimal itemTotal, UUID userId) {
        Coupon coupon = couponRepository.findByCodeIgnoreCaseAndActiveTrue(code).orElse(null);
        if (coupon == null) {
            return new CouponApplyResponse(false, code, BigDecimal.ZERO, "Invalid or expired coupon code");
        }
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(Instant.now())) {
            return new CouponApplyResponse(false, code, BigDecimal.ZERO, "This coupon has expired");
        }
        if (coupon.getMinOrderValue() != null && itemTotal.compareTo(coupon.getMinOrderValue()) < 0) {
            return new CouponApplyResponse(
                    false,
                    code,
                    BigDecimal.ZERO,
                    "You are not eligible for this coupon code");
        }
        if (FIRST_ORDER_CODE.equalsIgnoreCase(coupon.getCode())
                && userId != null
                && orderRepository.countByUserId(userId) > 0) {
            return new CouponApplyResponse(
                    false,
                    code,
                    BigDecimal.ZERO,
                    "You are not eligible for this coupon code");
        }

        BigDecimal discount = coupon.getType() == CouponType.PERCENT
                ? itemTotal.multiply(coupon.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : coupon.getValue();

        // A coupon can never discount more than the cart is worth.
        if (discount.compareTo(itemTotal) > 0) {
            discount = itemTotal;
        }

        return new CouponApplyResponse(true, coupon.getCode(), discount, "Coupon applied");
    }
}
