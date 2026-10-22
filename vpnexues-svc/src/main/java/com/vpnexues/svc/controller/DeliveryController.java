package com.vpnexues.svc.controller;

import com.vpnexues.svc.service.DeliveryAvailabilityService;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public delivery-availability probe. Applies EXACTLY the rules the checkout
 * gate (OrderService.checkout) enforces, so callers can check a city without
 * placing an order. Read-only, no PII in or out.
 */
@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryAvailabilityService deliveryAvailabilityService;

    @GetMapping("/availability")
    public Map<String, Object> availability(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country) {
        String normalizedCountry = country == null ? "" : country.trim().toUpperCase(Locale.ROOT);
        boolean serviceable = deliveryAvailabilityService.isServiceable(normalizedCountry, city);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("serviceable", serviceable);
        body.put("code", serviceable ? "DELIVERY_AVAILABLE" : "DELIVERY_NOT_AVAILABLE");
        body.put("country", normalizedCountry);
        body.put("city", city == null ? "" : city);
        return body;
    }
}
