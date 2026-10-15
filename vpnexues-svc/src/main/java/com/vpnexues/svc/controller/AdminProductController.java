package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.AdminProductDto;
import com.vpnexues.svc.dto.CreateProductRequest;
import com.vpnexues.svc.dto.PageResponse;
import com.vpnexues.svc.dto.UpdateProductRequest;
import com.vpnexues.svc.service.AdminAccessService;
import com.vpnexues.svc.service.AdminProductService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;
    private final AdminAccessService accessService;

    @GetMapping("/api/admin/products")
    public PageResponse<AdminProductDto> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UUID adminId) {
        accessService.requireRead(adminId, AdminAccessService.M_PRODUCTS);
        size = Math.min(Math.max(1, size), 200);
        Pageable pageable = PageRequest.of(page, size);
        return PageResponse.from(adminProductService.list(category, search, pageable));
    }

    @GetMapping("/api/admin/products/{id}")
    public AdminProductDto get(@PathVariable UUID id, @AuthenticationPrincipal UUID adminId) {
        accessService.requireRead(adminId, AdminAccessService.M_PRODUCTS);
        return adminProductService.get(id);
    }

    @PostMapping("/api/admin/products")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminProductDto create(@RequestBody @Valid CreateProductRequest req, @AuthenticationPrincipal UUID adminId) {
        accessService.requirePermission(adminId, AdminAccessService.M_PRODUCTS, AdminAccessService.P_EDIT);
        return adminProductService.create(req);
    }

    @PutMapping("/api/admin/products/{id}")
    public AdminProductDto update(
            @PathVariable UUID id, @RequestBody @Valid UpdateProductRequest req, @AuthenticationPrincipal UUID adminId) {
        accessService.requirePermission(adminId, AdminAccessService.M_PRODUCTS, AdminAccessService.P_EDIT);
        return adminProductService.update(id, req);
    }

    @DeleteMapping("/api/admin/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id, @AuthenticationPrincipal UUID adminId) {
        accessService.requirePermission(adminId, AdminAccessService.M_PRODUCTS, AdminAccessService.P_DELETE);
        adminProductService.deactivate(id);
    }
}
