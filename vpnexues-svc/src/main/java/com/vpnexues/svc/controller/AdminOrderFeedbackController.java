package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.OrderFeedbackDto;
import com.vpnexues.svc.service.AdminAccessService;
import com.vpnexues.svc.service.OrderFeedbackService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminOrderFeedbackController {

    private final OrderFeedbackService orderFeedbackService;
    private final AdminAccessService accessService;

    @GetMapping("/api/admin/order-feedback")
    public List<OrderFeedbackDto> list(
            @RequestParam(required = false) Short rating, @AuthenticationPrincipal UUID adminId) {
        accessService.requireRead(adminId, AdminAccessService.M_ORDERS);
        return orderFeedbackService.list(rating);
    }

    @DeleteMapping("/api/admin/order-feedback/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal UUID adminId) {
        accessService.requirePermission(adminId, AdminAccessService.M_ORDERS, AdminAccessService.P_DELETE);
        orderFeedbackService.delete(id);
    }
}
