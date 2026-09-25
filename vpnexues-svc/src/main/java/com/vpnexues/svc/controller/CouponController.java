package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.CouponApplyRequest;
import com.vpnexues.svc.dto.CouponApplyResponse;
import com.vpnexues.svc.security.CartOwnerResolver;
import com.vpnexues.svc.service.CartOwner;
import com.vpnexues.svc.service.CartService;
import com.vpnexues.svc.service.CouponService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final CartService cartService;
    private final CartOwnerResolver cartOwnerResolver;

    /** Preview-only: computes discount against the caller's own server-side cart total, never a client-supplied amount. */
    @PostMapping("/api/coupons/apply")
    public CouponApplyResponse apply(
            @RequestBody @Valid CouponApplyRequest req,
            @RequestParam(defaultValue = "SG") String country,
            HttpServletRequest request,
            HttpServletResponse response) {
        CartOwner owner = cartOwnerResolver.resolve(request, response);
        var cart = cartService.getCart(owner, country);
        return couponService.apply(req.code(), cart.itemTotal(), owner.userId());
    }
}
