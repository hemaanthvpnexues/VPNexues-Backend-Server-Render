package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.DashboardSummaryDto;
import com.vpnexues.svc.service.AdminAccessService;
import com.vpnexues.svc.service.DashboardSummaryService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardSummaryService dashboardSummaryService;
    private final AdminAccessService accessService;

    @GetMapping("/api/admin/dashboard/summary")
    public DashboardSummaryDto summary(@AuthenticationPrincipal UUID adminId) {
        accessService.requireRead(adminId, AdminAccessService.M_DASHBOARD);
        return dashboardSummaryService.get();
    }
}
