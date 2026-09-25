package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.AdminCustomerDto;
import com.vpnexues.svc.entity.User;
import com.vpnexues.svc.repository.CustomerOrderStats;
import com.vpnexues.svc.repository.OrderRepository;
import com.vpnexues.svc.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCustomerService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public List<AdminCustomerDto> list(String type, String country, String search) {
        Map<UUID, CustomerOrderStats> statsByUser = orderRepository.aggregateStatsByUser().stream()
                .collect(java.util.stream.Collectors.toMap(CustomerOrderStats::getUserId, Function.identity()));

        return userRepository.findAll().stream()
                .map(u -> toDto(u, statsByUser.get(u.getId())))
                .filter(dto -> !StringUtils.hasText(type) || dto.customerType().equalsIgnoreCase(type))
                .filter(dto -> !StringUtils.hasText(country) || country.equalsIgnoreCase(dto.countryCode()))
                .filter(dto -> !StringUtils.hasText(search) || matchesSearch(dto, search))
                .toList();
    }

    private boolean matchesSearch(AdminCustomerDto dto, String search) {
        String q = search.toLowerCase();
        return (dto.name() != null && dto.name().toLowerCase().contains(q))
                || (dto.email() != null && dto.email().toLowerCase().contains(q))
                || (dto.phone() != null && dto.phone().toLowerCase().contains(q));
    }

    private AdminCustomerDto toDto(User u, CustomerOrderStats stats) {
        int orderCount = stats == null ? 0 : stats.getOrderCount().intValue();
        BigDecimal totalSpent = stats == null || stats.getTotalSpent() == null ? BigDecimal.ZERO : stats.getTotalSpent();
        boolean isB2B = stats != null && stats.getB2bOrderCount() != null && stats.getB2bOrderCount() > 0;
        String countryCode = stats == null ? null : stats.getLastCountryCode();

        return new AdminCustomerDto(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getPhone(),
                countryCode,
                orderCount,
                totalSpent,
                isB2B ? "B2B" : "B2C",
                u.getCreatedAt());
    }
}
