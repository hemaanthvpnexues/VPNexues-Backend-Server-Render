package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.Payment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrderUserIdOrderByCreatedAtDesc(UUID userId);

    @Query(
            "select p from Payment p join fetch p.order o join fetch o.user order by p.createdAt desc")
    List<Payment> findAllWithOrderAndUser();
}
