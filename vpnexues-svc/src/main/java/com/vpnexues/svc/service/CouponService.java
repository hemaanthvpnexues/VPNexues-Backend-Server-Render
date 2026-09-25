package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.CouponApplyResponse;
import com.vpnexues.svc.entity.Coupon;
import com.vpnexues.svc.entity.CouponType;
import com.vpnexues.svc.repository.CouponRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponApplyResponse apply(String code, BigDecimal itemTotal) {
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
                    "Minimum order value of " + coupon.getMinOrderValue() + " required for this coupon");
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
