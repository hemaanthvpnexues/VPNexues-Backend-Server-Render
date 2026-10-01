package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.Order;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o LEFT JOIN FETCH o.user ORDER BY o.createdAt DESC")
    List<Order> findAllWithUser();

    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByOrderNumber(String orderNumber);

    /** First-time-only coupons (e.g. FIRST200) need a prior-order check. */
    long countByUserId(UUID userId);

    /** Per-user order aggregates for the admin Customers view (computed, not a stored table). */
    @Query("""
            SELECT o.user.id AS userId,
                   COUNT(o) AS orderCount,
                   COALESCE(SUM(o.grandTotal), 0) AS totalSpent,
                   SUM(CASE WHEN o.channel = com.vpnexues.svc.entity.OrderChannel.B2B THEN 1L ELSE 0L END) AS b2bOrderCount,
                   MAX(o.countryCode) AS lastCountryCode
            FROM Order o
            GROUP BY o.user.id
            """)
    List<CustomerOrderStats> aggregateStatsByUser();
}
