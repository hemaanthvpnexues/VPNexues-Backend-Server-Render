package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.AdminInventoryItemDto;
import com.vpnexues.svc.dto.AdminOrderSummaryDto;
import com.vpnexues.svc.dto.DashboardSummaryDto;
import com.vpnexues.svc.entity.Order;
import com.vpnexues.svc.entity.OrderStatus;
import com.vpnexues.svc.entity.User;
import com.vpnexues.svc.repository.OrderRepository;
import com.vpnexues.svc.repository.ProductRepository;
import com.vpnexues.svc.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardSummaryService {

    private static final int RECENT_ORDERS_LIMIT = 5;
    private static final int LOW_STOCK_LIMIT = 5;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryItemService inventoryItemService;

    public DashboardSummaryDto get() {
        List<Order> ordersByRecency = List.of();
        try {
            ordersByRecency = orderRepository.findAllWithUser();
        } catch (Exception ex) {
            // keep dashboard available despite DB/order errors
        }

        BigDecimal totalRevenue = BigDecimal.ZERO;
        try {
            totalRevenue = ordersByRecency.stream()
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                    .map(Order::getGrandTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception ex) {
            // ignore calculation errors
        }

        List<AdminOrderSummaryDto> recentOrders = List.of();
        try {
            recentOrders = ordersByRecency.stream()
                    .limit(RECENT_ORDERS_LIMIT)
                    .map(this::toOrderSummaryDto)
                    .toList();
        } catch (Exception ex) {
            // ignore mapping errors
        }

        List<AdminInventoryItemDto> lowStockItems = List.of();
        try {
            lowStockItems = inventoryItemService.list(null, null).stream()
                    .filter(i -> !"IN_STOCK".equals(i.status()))
                    .sorted(Comparator.comparingInt(AdminInventoryItemDto::quantity))
                    .limit(LOW_STOCK_LIMIT)
                    .toList();
        } catch (Exception ex) {
            // ignore inventory errors to keep dashboard available
        }

        long userCount = 0;
        try {
            userCount = userRepository.count();
        } catch (Exception ex) {
            // ignore
        }

        long productCount = 0;
        try {
            productCount = productRepository.countByActiveTrue();
        } catch (Exception ex) {
            // ignore
        }

        return new DashboardSummaryDto(
                totalRevenue,
                ordersByRecency.size(),
                userCount,
                productCount,
                recentOrders,
                lowStockItems);
    }

    private AdminOrderSummaryDto toOrderSummaryDto(Order order) {
        User user = order.getUser();
        return new AdminOrderSummaryDto(
                order.getId(),
                order.getOrderNumber(),
                user != null ? user.getName() : "Unknown",
                user != null ? user.getPhone() : "",
                order.getCountryCode(),
                order.getChannel() != null ? order.getChannel().name() : "SHOP",
                order.getStatus() != null ? order.getStatus().name() : "",
                order.getGrandTotal(),
                order.getCreatedAt());
    }
}
