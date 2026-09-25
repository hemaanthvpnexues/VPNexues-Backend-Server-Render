package com.vpnexues.svc.repository;

import java.math.BigDecimal;
import java.util.UUID;

/** Interface projection for OrderRepository.aggregateStatsByUser() — Spring Data maps by getter-matching JPQL alias. */
public interface CustomerOrderStats {
    UUID getUserId();

    Long getOrderCount();

    BigDecimal getTotalSpent();

    Long getB2bOrderCount();

    /** Approximation, not "most recent" — MAX() is the only cheap way to pick one country per user in a GROUP BY. */
    String getLastCountryCode();
}
