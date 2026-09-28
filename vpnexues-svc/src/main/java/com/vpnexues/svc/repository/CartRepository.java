package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.Cart;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    /**
     * Eager-fetches items + products in the SAME statement as the cart row.
     *
     * <p>Both associations are LAZY, so without this graph a cart read costs 1 query for the cart + 1 for the
     * items + one per distinct product (batching only helps if every proxy is seen at once). Each round trip
     * costs ~150-200 ms because the app runs in Oregon and the database in Mumbai, so N+1 here is what made
     * an 8-item cart read take ~3.4 s.
     */
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByUserId(UUID userId);

    /** Guest-cart equivalent of {@link #findByUserId(UUID)} - same single-statement guarantee. */
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByGuestToken(String guestToken);

    /**
     * Same lookup as {@link #findByUserId(UUID)} but takes a row-level {@code SELECT ... FOR UPDATE} lock.
     *
     * <p>Cart mutations (add / set-box / update qty / remove) run as read-modify-write on the same cart row.
     * Without this lock two concurrent requests both read the pre-existing item list and the slower commit
     * silently drops the other request's item - the "added one after another, one item disappeared" bug.
     * Every mutation must therefore start by loading the cart through one of these locked finders, which
     * serializes writers on that cart for the rest of the transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cart c where c.user.id = :userId")
    Optional<Cart> findByUserIdForUpdate(@Param("userId") UUID userId);

    /** Guest-cart equivalent of {@link #findByUserIdForUpdate(UUID)}. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cart c where c.guestToken = :guestToken")
    Optional<Cart> findByGuestTokenForUpdate(@Param("guestToken") String guestToken);
}
