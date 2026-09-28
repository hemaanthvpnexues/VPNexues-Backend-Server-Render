package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.AdminPaymentDto;
import com.vpnexues.svc.entity.Payment;
import com.vpnexues.svc.repository.PaymentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentRepository paymentRepository;

    @GetMapping("/api/admin/payments")
    public List<AdminPaymentDto> list() {
        return paymentRepository.findAllWithOrderAndUser().stream().map(this::toDto).toList();
    }

    private AdminPaymentDto toDto(Payment payment) {
        var order = payment.getOrder();
        var user = order.getUser();
        return new AdminPaymentDto(
                payment.getId(),
                order.getId(),
                order.getOrderNumber(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                order.getCountryCode(),
                payment.getMethod(),
                payment.getStatus().name(),
                payment.getAmount(),
                payment.getPaidAt() != null ? payment.getPaidAt() : payment.getCreatedAt(),
                order.getStatus().name());
    }
}
