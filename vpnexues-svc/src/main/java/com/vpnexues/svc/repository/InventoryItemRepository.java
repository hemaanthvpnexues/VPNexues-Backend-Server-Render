package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.InventoryItem;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    boolean existsByProductId(UUID productId);

    java.util.Optional<InventoryItem> findByProductId(UUID productId);

    List<InventoryItem> findAllByProductIdIn(Collection<UUID> productIds);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM InventoryItem i LEFT JOIN FETCH i.product")
    List<InventoryItem> findAllWithProduct();
}
