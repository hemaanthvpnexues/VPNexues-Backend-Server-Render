package com.vpnexues.svc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vpnexues.svc.dto.OrderDto;
import com.vpnexues.svc.entity.Order;
import com.vpnexues.svc.entity.OrderStatus;
import com.vpnexues.svc.entity.User;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.OrderRepository;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for customer self-service order cancellation — no DB, no Spring context. */
class OrderServiceTest {

    private static final UUID OWNER_ID = UUID.randomUUID();

    private final Map<UUID, Order> orders = new HashMap<>();
    private OrderService service;

    @BeforeEach
    void setUp() {
        OrderRepository repository = (OrderRepository) Proxy.newProxyInstance(
                OrderRepository.class.getClassLoader(),
                new Class<?>[] { OrderRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByIdAndUserId" -> {
                        Order order = orders.get(args[0]);
                        yield order != null && order.getUser().getId().equals(args[1])
                                ? Optional.of(order)
                                : Optional.empty();
                    }
                    case "save" -> args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        service = new OrderService(repository, null, null, null, null, null, null, null);
    }

    @Test
    void cancelWithinWindowMarksOrderCancelled() {
        Order order = placeOrder(OrderStatus.PLACED, Instant.now());

        OrderDto dto = service.cancelForUser(order.getId(), OWNER_ID);

        assertEquals("CANCELLED", dto.status());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void cancelAfterOneHourIsRejected() {
        Order order = placeOrder(OrderStatus.PLACED, Instant.now().minus(Duration.ofHours(2)));

        assertThrows(BadRequestException.class, () -> service.cancelForUser(order.getId(), OWNER_ID));
        assertEquals(OrderStatus.PLACED, order.getStatus());
    }

    @Test
    void cancelShippedOrderIsRejectedByStateMachine() {
        Order order = placeOrder(OrderStatus.SHIPPED, Instant.now());

        assertThrows(BadRequestException.class, () -> service.cancelForUser(order.getId(), OWNER_ID));
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
    }

    @Test
    void cancelAlreadyCancelledOrderIsRejected() {
        Order order = placeOrder(OrderStatus.CANCELLED, Instant.now());

        assertThrows(BadRequestException.class, () -> service.cancelForUser(order.getId(), OWNER_ID));
    }

    @Test
    void cancelAnotherUsersOrderIsNotFound() {
        Order order = placeOrder(OrderStatus.PLACED, Instant.now());

        assertThrows(
                NotFoundException.class,
                () -> service.cancelForUser(order.getId(), UUID.randomUUID()));
        assertEquals(OrderStatus.PLACED, order.getStatus());
    }

    private Order placeOrder(OrderStatus status, Instant createdAt) {
        User user = new User();
        user.setId(OWNER_ID);

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setOrderNumber("HB-100001");
        order.setUser(user);
        order.setStatus(status);
        order.setCreatedAt(createdAt);
        orders.put(order.getId(), order);
        return order;
    }
}
