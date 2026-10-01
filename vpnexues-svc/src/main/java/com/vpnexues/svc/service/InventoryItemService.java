package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.AdminInventoryItemDto;
import com.vpnexues.svc.dto.UpdateInventoryRequest;
import com.vpnexues.svc.entity.InventoryItem;
import com.vpnexues.svc.entity.Product;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.InventoryItemRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryItemService {

    private static final int DEFAULT_REORDER_THRESHOLD = 10;

    private final InventoryItemRepository inventoryItemRepository;

    /** Called by AdminProductService right after a product is created — no-ops if a row already exists. */
    public void createDefaultFor(Product product) {
        if (inventoryItemRepository.existsByProductId(product.getId())) {
            return;
        }
        InventoryItem item = new InventoryItem();
        item.setProduct(product);
        item.setQuantity(0);
        item.setReorderThreshold(DEFAULT_REORDER_THRESHOLD);
        inventoryItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<AdminInventoryItemDto> list(String category, String status) {
        return inventoryItemRepository.findAllWithProduct().stream()
                .filter(i -> !StringUtils.hasText(category) || i.getProduct().getCategory().equalsIgnoreCase(category))
                .map(this::toDto)
                .filter(dto -> !StringUtils.hasText(status) || dto.status().equalsIgnoreCase(status))
                .toList();
    }

    public AdminInventoryItemDto update(UUID id, UpdateInventoryRequest req) {
        InventoryItem item = inventoryItemRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Inventory item not found: " + id));
        item.setQuantity(req.quantity());
        item.setReorderThreshold(req.reorderThreshold());
        return toDto(inventoryItemRepository.save(item));
    }

    private AdminInventoryItemDto toDto(InventoryItem item) {
        Product product = item.getProduct();
        String status;
        if (item.getQuantity() <= 0) {
            status = "OUT_OF_STOCK";
        } else if (item.getQuantity() <= item.getReorderThreshold()) {
            status = "LOW_STOCK";
        } else {
            status = "IN_STOCK";
        }
        return new AdminInventoryItemDto(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getCategory(),
                product.getImageUrl(),
                item.getQuantity(),
                item.getReorderThreshold(),
                status,
                item.getUpdatedAt());
    }
}
