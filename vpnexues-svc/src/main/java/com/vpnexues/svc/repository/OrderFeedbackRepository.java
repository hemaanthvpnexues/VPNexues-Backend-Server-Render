package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.OrderFeedback;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderFeedbackRepository extends JpaRepository<OrderFeedback, UUID> {

    List<OrderFeedback> findAllByOrderByCreatedAtDesc();

    List<OrderFeedback> findByRatingOrderByCreatedAtDesc(short rating);
}
