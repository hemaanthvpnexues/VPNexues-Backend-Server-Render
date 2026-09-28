package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.Cart;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByUserId(UUID userId);

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
