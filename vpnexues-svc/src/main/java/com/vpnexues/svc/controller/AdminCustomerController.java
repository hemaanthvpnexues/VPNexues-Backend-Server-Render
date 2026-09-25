package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.AdminCustomerDto;
import com.vpnexues.svc.service.AdminCustomerService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;

    @GetMapping("/api/admin/customers")
    public List<AdminCustomerDto> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String search) {
        return adminCustomerService.list(type, country, search);
    }
}
