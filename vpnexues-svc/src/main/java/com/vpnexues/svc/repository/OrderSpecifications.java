package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.Order;
import com.vpnexues.svc.entity.OrderChannel;
import com.vpnexues.svc.entity.OrderStatus;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Optional-filter building blocks for the admin Orders list. Each always returns a non-null
 * Specification — an unset filter resolves to cb.conjunction() (always-true) rather than null,
 * since this Spring Data JPA version's Specification.where()/.and() reject null arguments
 * (older versions tolerated null as "no-op"; this one throws IllegalArgumentException).
 */
public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Order> hasChannel(OrderChannel channel) {
        return (root, query, cb) -> channel == null ? cb.conjunction() : cb.equal(root.get("channel"), channel);
    }

    public static Specification<Order> hasCountry(String countryCode) {
        return (root, query, cb) -> StringUtils.hasText(countryCode)
                ? cb.equal(root.get("countryCode"), countryCode)
                : cb.conjunction();
    }

    public static Specification<Order> createdFrom(Instant from) {
        return (root, query, cb) -> from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Order> createdBefore(Instant to) {
        return (root, query, cb) -> to == null ? cb.conjunction() : cb.lessThan(root.get("createdAt"), to);
    }
}
