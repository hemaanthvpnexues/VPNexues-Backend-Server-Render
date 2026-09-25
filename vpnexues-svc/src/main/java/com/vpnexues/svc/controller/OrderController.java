package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.CheckoutRequest;
import com.vpnexues.svc.dto.OrderDto;
import com.vpnexues.svc.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/api/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto checkout(
            @AuthenticationPrincipal UUID userId,
            @RequestBody @Valid CheckoutRequest req,
            @RequestParam(defaultValue = "SG") String country) {
        return orderService.checkout(userId, country, req);
    }

    @GetMapping("/api/orders")
    public List<OrderDto> list(@AuthenticationPrincipal UUID userId) {
        return orderService.listForUser(userId);
    }

    @GetMapping("/api/orders/{id}")
    public OrderDto get(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        return orderService.getForUser(id, userId);
    }

    @GetMapping("/api/orders/{id}/tracking")
    public OrderDto tracking(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        // Phase 1: tracking view reuses the order detail (status only); a dedicated
        // timeline/tracking-events model is deferred — see TASKS.md Phase 2.
        return orderService.getForUser(id, userId);
    }
}
