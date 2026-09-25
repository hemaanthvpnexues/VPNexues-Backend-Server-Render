package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.AddCartItemRequest;
import com.vpnexues.svc.dto.CartDto;
import com.vpnexues.svc.dto.UpdateCartItemRequest;
import com.vpnexues.svc.security.CartOwnerResolver;
import com.vpnexues.svc.service.CartOwner;
import com.vpnexues.svc.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CartOwnerResolver cartOwnerResolver;

    @GetMapping("/api/cart")
    public CartDto get(
            @RequestParam(defaultValue = "SG") String country, HttpServletRequest request, HttpServletResponse response) {
        CartOwner owner = cartOwnerResolver.resolve(request, response);
        return cartService.getCart(owner, country);
    }

    @PostMapping("/api/cart/items")
    public CartDto addItem(
            @RequestBody @Valid AddCartItemRequest req,
            @RequestParam(defaultValue = "SG") String country,
            HttpServletRequest request,
            HttpServletResponse response) {
        CartOwner owner = cartOwnerResolver.resolve(request, response);
        if (req.boxType() != null) {
            return cartService.setBoxItem(owner, req.productId(), req.boxType(), req.qty(), req.weightKg(), country);
        }
        return cartService.addItem(owner, req.productId(), req.qty(), country);
    }

    @PutMapping("/api/cart/items/{itemId}")
    public CartDto updateItem(
            @PathVariable UUID itemId,
            @RequestBody @Valid UpdateCartItemRequest req,
            HttpServletRequest request,
            HttpServletResponse response) {
        CartOwner owner = cartOwnerResolver.resolve(request, response);
        return cartService.updateItemQty(owner, itemId, req.qty(), req.weightKg());
    }

    @DeleteMapping("/api/cart/items/{itemId}")
    public CartDto removeItem(@PathVariable UUID itemId, HttpServletRequest request, HttpServletResponse response) {
        CartOwner owner = cartOwnerResolver.resolve(request, response);
        return cartService.removeItem(owner, itemId);
    }

    @DeleteMapping("/api/cart")
    public CartDto clear(HttpServletRequest request, HttpServletResponse response) {
        CartOwner owner = cartOwnerResolver.resolve(request, response);
        return cartService.clear(owner);
    }
}
