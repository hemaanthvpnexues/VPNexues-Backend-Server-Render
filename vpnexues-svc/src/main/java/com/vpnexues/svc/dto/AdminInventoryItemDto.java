package com.vpnexues.svc.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminInventoryItemDto(
        UUID id,
        UUID productId,
        String productName,
        String sku,
        String category,
        String imageUrl,
        int quantity,
        int reorderThreshold,
        String status,
        Instant updatedAt) {
}
