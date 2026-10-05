package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.AddressDto;
import com.vpnexues.svc.dto.AdminOrderDetailDto;
import com.vpnexues.svc.dto.AdminOrderSummaryDto;
import com.vpnexues.svc.dto.OrderItemDto;
import com.vpnexues.svc.dto.UserDto;
import com.vpnexues.svc.entity.Address;
import com.vpnexues.svc.entity.AdminRole;
import com.vpnexues.svc.entity.Order;
import com.vpnexues.svc.entity.OrderChannel;
import com.vpnexues.svc.entity.OrderStatus;
import com.vpnexues.svc.entity.User;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.AdminUserRepository;
import com.vpnexues.svc.repository.OrderRepository;
import com.vpnexues.svc.repository.OrderSpecifications;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final AdminUserRepository adminUserRepository;

    public Page<AdminOrderSummaryDto> list(
            OrderStatus status,
            OrderChannel channel,
            String countryCode,
            Instant createdFrom,
            Instant createdBefore,
            Pageable pageable) {
        Specification<Order> spec = Specification.<Order>where(OrderSpecifications.hasStatus(status))
                .and(OrderSpecifications.hasChannel(channel))
                .and(OrderSpecifications.hasCountry(countryCode))
                .and(OrderSpecifications.createdFrom(createdFrom))
                .and(OrderSpecifications.createdBefore(createdBefore));
        return orderRepository.findAll(spec, pageable).map(this::toSummaryDto);
    }

    public AdminOrderDetailDto get(UUID id) {
        return toDetailDto(getEntity(id));
    }

    @Transactional
    public AdminOrderDetailDto updateStatus(UUID id, OrderStatus status, UUID requestingAdminId) {
        Order order = getEntity(id);
        requireOrderCountryScope(order, requestingAdminId);
        if (!order.getStatus().canTransitionTo(status)) {
            throw new BadRequestException(
                    "Cannot transition order from " + order.getStatus() + " to " + status);
        }
        order.setStatus(status);
        return toDetailDto(orderRepository.save(order));
    }

    /** SUPER_ADMIN manages all countries; every other admin is scoped to their own country. */
    private void requireOrderCountryScope(Order order, UUID requestingAdminId) {
        var admin = requestingAdminId == null
                ? null
                : adminUserRepository.findById(requestingAdminId).orElse(null);
        if (admin == null) {
            throw new AccessDeniedException("Access denied");
        }
        if (admin.getRole() == AdminRole.SUPER_ADMIN) {
            return;
        }
        String orderCountry = order.getCountryCode();
        String adminCountry = admin.getCountryCode();
        if (orderCountry == null || adminCountry == null || !orderCountry.equalsIgnoreCase(adminCountry)) {
            throw new AccessDeniedException("You can only update orders from your own country.");
        }
    }

    private Order getEntity(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }

    private AdminOrderSummaryDto toSummaryDto(Order order) {
        User user = order.getUser();
        return new AdminOrderSummaryDto(
                order.getId(),
                order.getOrderNumber(),
                user.getName(),
                user.getPhone(),
                order.getCountryCode(),
                order.getChannel().name(),
                order.getStatus().name(),
                order.getGrandTotal(),
                order.getCreatedAt());
    }

    private AdminOrderDetailDto toDetailDto(Order order) {
        User user = order.getUser();
        Address address = order.getAddress();
        List<OrderItemDto> items = order.getItems().stream()
                .map(i -> new OrderItemDto(
                        i.getName(), i.getImageUrl(), i.getBoxType(), i.getQty(), i.getWeightKg(), i.getUnitPrice(), i.getLineTotal()))
                .toList();
        UserDto buyer = UserDto.of(user);
        AddressDto addressDto = new AddressDto(
                address.getId(),
                address.getLabel(),
                address.getName(),
                address.getPhone(),
                address.getAddressLine(),
                address.getFlat(),
                address.getLandmark(),
                address.getCity(),
                address.getPincode(),
                address.getState(),
                address.isDefault(),
                address.getLat(),
                address.getLng());

        return new AdminOrderDetailDto(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getChannel().name(),
                buyer,
                addressDto,
                items,
                order.getItemTotal(),
                order.getDiscount(),
                order.getBoxDiscount(),
                order.getDeliveryFee(),
                order.getTax(),
                order.getTip(),
                order.getGrandTotal(),
                order.getPaymentMethod(),
                order.getCouponCode(),
                order.getCountryCode(),
                order.getCreatedAt());
    }
}
