package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.AdminInventoryItemDto;
import com.vpnexues.svc.dto.UpdateInventoryRequest;
import com.vpnexues.svc.service.InventoryItemService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminInventoryController {

    private final InventoryItemService inventoryItemService;

    @GetMapping("/api/admin/inventory")
    public List<AdminInventoryItemDto> list(
            @RequestParam(required = false) String category, @RequestParam(required = false) String status) {
        return inventoryItemService.list(category, status);
    }

    @PutMapping("/api/admin/inventory/{id}")
    public AdminInventoryItemDto update(@PathVariable UUID id, @RequestBody @Valid UpdateInventoryRequest req) {
        return inventoryItemService.update(id, req);
    }
}
