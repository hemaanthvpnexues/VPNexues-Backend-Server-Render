package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.StoreSettingsDto;
import com.vpnexues.svc.dto.UpdateStoreSettingsRequest;
import com.vpnexues.svc.service.StoreSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminSettingsController {

    private final StoreSettingsService storeSettingsService;

    @GetMapping("/api/admin/settings")
    public StoreSettingsDto get() {
        return storeSettingsService.get();
    }

    /** Global pricing knobs = super-admin only (was open to every SUB_ADMIN). */
    @PutMapping("/api/admin/settings")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public StoreSettingsDto update(@RequestBody @Valid UpdateStoreSettingsRequest req) {
        return storeSettingsService.update(req);
    }
}
