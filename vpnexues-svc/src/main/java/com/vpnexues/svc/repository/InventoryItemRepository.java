package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.InventoryItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    boolean existsByProductId(UUID productId);

    java.util.Optional<InventoryItem> findByProductId(UUID productId);
}
