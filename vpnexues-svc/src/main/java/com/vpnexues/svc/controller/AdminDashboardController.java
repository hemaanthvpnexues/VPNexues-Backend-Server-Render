package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.DashboardSummaryDto;
import com.vpnexues.svc.service.DashboardSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardSummaryService dashboardSummaryService;

    @GetMapping("/api/admin/dashboard/summary")
    public DashboardSummaryDto summary() {
        return dashboardSummaryService.get();
    }
}
