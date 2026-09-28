package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.Payment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrderUserIdOrderByCreatedAtDesc(UUID userId);
}
