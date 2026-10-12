package com.vpnexues.svc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vpnexues.svc.dto.AdminOrderDetailDto;
import com.vpnexues.svc.entity.Address;
import com.vpnexues.svc.entity.AdminRole;
import com.vpnexues.svc.entity.AdminUser;
import com.vpnexues.svc.entity.Order;
import com.vpnexues.svc.entity.OrderStatus;
import com.vpnexues.svc.entity.User;
import com.vpnexues.svc.repository.AdminUserRepository;
import com.vpnexues.svc.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

/**
 * QA-02 regression: sub-admin country scope must be enforced on list() and get(), not just
 * updateStatus(). SUPER_ADMIN sees everything; every other admin is pinned to their own country.
 */
class AdminOrderServiceTest {

    private OrderRepository orderRepository;
    private AdminUserRepository adminUserRepository;
    private AdminOrderService service;

    private UUID superAdminId;
    private UUID indiaAdminId;
    private UUID singaporeAdminId;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        adminUserRepository = mock(AdminUserRepository.class);
        service = new AdminOrderService(orderRepository, adminUserRepository);

        superAdminId = UUID.randomUUID();
        indiaAdminId = UUID.randomUUID();
        singaporeAdminId = UUID.randomUUID();

        when(adminUserRepository.findById(superAdminId)).thenReturn(Optional.of(
                admin(superAdminId, AdminRole.SUPER_ADMIN, null)));
        when(adminUserRepository.findById(indiaAdminId)).thenReturn(Optional.of(
                admin(indiaAdminId, AdminRole.SUB_ADMIN, "IN")));
        when(adminUserRepository.findById(singaporeAdminId)).thenReturn(Optional.of(
                admin(singaporeAdminId, AdminRole.SUB_ADMIN, "SG")));
    }

    // ---------- list() ----------

    @Test
    void superAdminList_canFilterAnyCountryOrNullForAll() {
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        // null country = all countries — allowed for SUPER_ADMIN
        service.list(null, null, null, null, null, null, Pageable.unpaged(), superAdminId);

        // foreign country filter — allowed for SUPER_ADMIN
        service.list(null, null, "SG", null, null, null, Pageable.unpaged(), superAdminId);
    }

    @Test
    void subAdminList_withoutCountryParam_isPinnedToOwnCountry() {
        Page<Order> emptyPage = Page.empty();
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // India sub-admin, no ?country= → scope must resolve to IN (not all-countries)
        var result = service.list(null, null, null, null, null, null, Pageable.unpaged(), indiaAdminId);
        assertEquals(0, result.getTotalElements());
        verify(orderRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void subAdminList_requestingOwnCountry_isAllowed() {
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.list(null, null, "IN", null, null, null, Pageable.unpaged(), indiaAdminId);
    }

    @Test
    void subAdminList_requestingForeignCountry_isRejected() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                service.list(null, null, "SG", null, null, null, Pageable.unpaged(), indiaAdminId));
        assertEquals("You can only view orders from your own country.", ex.getMessage());
        verify(orderRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void subAdminList_countryCaseInsensitive_isAllowed() {
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.list(null, null, "in", null, null, null, Pageable.unpaged(), indiaAdminId);
    }

    @Test
    void subAdminList_withoutAssignedCountry_isRejected() {
        UUID rogueId = UUID.randomUUID();
        when(adminUserRepository.findById(rogueId)).thenReturn(Optional.of(
                admin(rogueId, AdminRole.SUB_ADMIN, null)));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                service.list(null, null, null, null, null, null, Pageable.unpaged(), rogueId));
        assertEquals("Access denied: your admin account has no country assigned.", ex.getMessage());
    }

    @Test
    void list_withUnknownAdmin_isRejected() {
        UUID unknownId = UUID.randomUUID();
        when(adminUserRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () ->
                service.list(null, null, null, null, null, null, Pageable.unpaged(), unknownId));
    }

    @Test
    void list_withNullAdminId_isRejected() {
        assertThrows(AccessDeniedException.class, () ->
                service.list(null, null, null, null, null, null, Pageable.unpaged(), null));
    }

    // ---------- get() ----------

    @Test
    void subAdminGet_ownCountryOrder_isAllowed() {
        Order order = order(UUID.randomUUID(), "IN", OrderStatus.PLACED);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        AdminOrderDetailDto dto = service.get(order.getId(), indiaAdminId);
        assertEquals("IN", dto.countryCode());
    }

    @Test
    void subAdminGet_foreignOrder_isRejected() {
        Order order = order(UUID.randomUUID(), "SG", OrderStatus.PLACED);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                service.get(order.getId(), indiaAdminId));
        assertEquals("You can only access orders from your own country.", ex.getMessage());
    }

    @Test
    void superAdminGet_anyOrder_isAllowed() {
        Order order = order(UUID.randomUUID(), "SG", OrderStatus.PLACED);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        AdminOrderDetailDto dto = service.get(order.getId(), superAdminId);
        assertEquals("SG", dto.countryCode());
    }

    @Test
    void get_withNullAdminId_isRejected() {
        Order order = order(UUID.randomUUID(), "IN", OrderStatus.PLACED);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class, () -> service.get(order.getId(), null));
    }

    // ---------- updateStatus() regression ----------

    @Test
    void updateStatus_subAdmin_foreignOrder_stillRejected() {
        Order order = order(UUID.randomUUID(), "SG", OrderStatus.PLACED);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class, () ->
                service.updateStatus(order.getId(), OrderStatus.CONFIRMED, indiaAdminId));
    }

    @Test
    void updateStatus_subAdmin_ownCountryOrder_stillWorks() {
        Order order = order(UUID.randomUUID(), "IN", OrderStatus.PLACED);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminOrderDetailDto dto = service.updateStatus(order.getId(), OrderStatus.CONFIRMED, indiaAdminId);
        assertEquals("CONFIRMED", dto.status());
    }

    // ---------- helpers ----------

    private AdminUser admin(UUID id, AdminRole role, String country) {
        AdminUser a = new AdminUser();
        a.setId(id);
        a.setRole(role);
        a.setCountryCode(country);
        return a;
    }

    private Order order(UUID id, String country, OrderStatus status) {
        User user = new User();
        user.setPhone("+919000000000");
        user.setName("Test Buyer");

        Address address = new Address();
        address.setUser(user);
        address.setLabel("Home");
        address.setName("Test Buyer");
        address.setPhone("+919000000000");
        address.setAddressLine("12 Test Street");
        address.setCity("Chennai");
        address.setPincode("600001");
        address.setState("Tamil Nadu");

        Order o = new Order();
        o.setId(id);
        o.setOrderNumber("VPX-" + id.toString().substring(0, 8));
        o.setUser(user);
        o.setAddress(address);
        o.setCountryCode(country);
        o.setStatus(status);
        o.setGrandTotal(new BigDecimal("100.00"));
        o.setPaymentMethod("COD");
        o.setCreatedAt(Instant.now());
        return o;
    }
}
