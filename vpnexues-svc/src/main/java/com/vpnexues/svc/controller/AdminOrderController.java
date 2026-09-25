package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.AdminOrderDetailDto;
import com.vpnexues.svc.dto.AdminOrderSummaryDto;
import com.vpnexues.svc.dto.PageResponse;
import com.vpnexues.svc.dto.UpdateOrderStatusRequest;
import com.vpnexues.svc.entity.OrderChannel;
import com.vpnexues.svc.entity.OrderStatus;
import com.vpnexues.svc.service.AdminOrderService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping("/api/admin/orders")
    public PageResponse<AdminOrderSummaryDto> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) OrderChannel channel,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(Math.max(1, size), 200);
        Instant createdFrom = dateFrom == null ? null : dateFrom.atStartOfDay(ZoneOffset.UTC).toInstant();
        // Exclusive upper bound one day past dateTo so the whole day is included.
        Instant createdBefore =
                dateTo == null ? null : dateTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Pageable pageable = PageRequest.of(page, size);
        OrderStatus orderStatus = null;
        try {
            if (status != null && !status.isEmpty()) {
                orderStatus = OrderStatus.valueOf(status);
            }
        } catch (IllegalArgumentException e) {
            orderStatus = null;
        }
        return PageResponse.from(
                adminOrderService.list(orderStatus, channel, country, createdFrom, createdBefore, pageable));
    }

    @GetMapping("/api/admin/orders/{id}")
    public AdminOrderDetailDto get(@PathVariable UUID id) {
        return adminOrderService.get(id);
    }

    @PutMapping("/api/admin/orders/{id}/status")
    public AdminOrderDetailDto updateStatus(@PathVariable UUID id, @RequestBody @Valid UpdateOrderStatusRequest req) {
        return adminOrderService.updateStatus(id, req.status());
    }
}
