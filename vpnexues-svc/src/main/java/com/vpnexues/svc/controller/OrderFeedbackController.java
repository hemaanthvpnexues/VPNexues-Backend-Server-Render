package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.CreateOrderFeedbackRequest;
import com.vpnexues.svc.dto.OrderFeedbackDto;
import com.vpnexues.svc.service.OrderFeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public submission endpoint — backs the Order Success page's feedback popup. */
@RestController
@RequiredArgsConstructor
public class OrderFeedbackController {

    private final OrderFeedbackService orderFeedbackService;

    @PostMapping("/api/order-feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderFeedbackDto create(@RequestBody @Valid CreateOrderFeedbackRequest req) {
        return orderFeedbackService.create(req);
    }
}
