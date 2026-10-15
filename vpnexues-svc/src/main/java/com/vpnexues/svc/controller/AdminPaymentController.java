package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.AdminPaymentDto;
import com.vpnexues.svc.entity.Payment;
import com.vpnexues.svc.repository.PaymentRepository;
import com.vpnexues.svc.service.AdminAccessService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentRepository paymentRepository;
    private final AdminAccessService accessService;

    @GetMapping("/api/admin/payments")
    public List<AdminPaymentDto> list(@AuthenticationPrincipal UUID adminId) {
        accessService.requireRead(adminId, AdminAccessService.M_PAYMENTS);
        return paymentRepository.findAllWithOrderAndUser().stream().map(this::toDto).toList();
    }

    private AdminPaymentDto toDto(Payment payment) {
        var order = payment.getOrder();
        // Guest orders have no user — pass nulls through (the admin UI already
        // falls back to phone/email/Unknown) instead of throwing NPE -> 500.
        var user = order.getUser();
        return new AdminPaymentDto(
                payment.getId(),
                order.getId(),
                order.getOrderNumber(),
                user == null ? null : user.getName(),
                user == null ? null : user.getPhone(),
                user == null ? null : user.getEmail(),
                order.getCountryCode(),
                payment.getMethod(),
                payment.getStatus().name(),
                payment.getAmount(),
                payment.getPaidAt() != null ? payment.getPaidAt() : payment.getCreatedAt(),
                order.getStatus().name());
    }
}
