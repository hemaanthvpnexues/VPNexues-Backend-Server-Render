package com.vpnexues.svc.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryDto(
        BigDecimal totalRevenue,
        long totalOrders,
        long totalCustomers,
        long totalProducts,
        List<AdminOrderSummaryDto> recentOrders,
        List<AdminInventoryItemDto> lowStockItems) {
}
