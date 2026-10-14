package com.vpnexues.svc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vpnexues.svc.dto.CreateOrderFeedbackRequest;
import com.vpnexues.svc.dto.OrderFeedbackDto;
import com.vpnexues.svc.entity.OrderFeedback;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.OrderFeedbackRepository;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for post-order feedback capture — no DB, no Spring context. */
class OrderFeedbackServiceTest {

    private final Map<UUID, OrderFeedback> rows = new ConcurrentHashMap<>();
    private OrderFeedbackService service;

    @BeforeEach
    void setUp() {
        OrderFeedbackRepository repository = (OrderFeedbackRepository) Proxy.newProxyInstance(
                OrderFeedbackRepository.class.getClassLoader(),
                new Class<?>[] { OrderFeedbackRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "save" -> {
                        OrderFeedback f = (OrderFeedback) args[0];
                        if (f.getId() == null) f.setId(UUID.randomUUID());
                        rows.put(f.getId(), f);
                        yield f;
                    }
                    case "findAllByOrderByCreatedAtDesc" -> rows.values().stream()
                            .sorted(newestFirst())
                            .collect(Collectors.toList());
                    case "findByRatingOrderByCreatedAtDesc" -> rows.values().stream()
                            .filter(f -> f.getRating() == (Short) args[0])
                            .sorted(newestFirst())
                            .collect(Collectors.toList());
                    case "existsById" -> rows.containsKey(args[0]);
                    case "deleteById" -> {
                        rows.remove(args[0]);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        service = new OrderFeedbackService(repository);
    }

    private static Comparator<OrderFeedback> newestFirst() {
        return Comparator.comparing(
                OrderFeedback::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed();
    }

    @Test
    void createStoresRatingAndNormalizesTagsAndComment() {
        OrderFeedbackDto dto = service.create(new CreateOrderFeedbackRequest(
                " HB-560002 ", 5, List.of("freshness", " delivery ", "", "freshness"),
                "  Great produce!  ", "  Fresh and on time  ", "IN", null, null));

        assertEquals("HB-560002", dto.orderNumber());
        assertEquals(5, dto.rating());
        assertEquals(List.of("freshness", "delivery"), dto.tags());
        assertEquals("Great produce!", dto.comment());
        assertEquals("Fresh and on time", dto.headline());
        assertEquals("IN", dto.countryCode());
    }

    @Test
    void createWithMinimalPayloadLeavesOptionalsEmpty() {
        OrderFeedbackDto dto = service.create(
                new CreateOrderFeedbackRequest(null, 3, null, "  ", null, null, null, null));

        assertNull(dto.orderNumber());
        assertEquals(3, dto.rating());
        assertTrue(dto.tags().isEmpty());
        assertNull(dto.comment());
        assertNull(dto.headline());
    }

    @Test
    void listOrdersNewestFirstAndFiltersByRating() {
        seed("HB-1", (short) 5, Instant.parse("2026-10-12T10:00:00Z"));
        seed("HB-2", (short) 2, Instant.parse("2026-10-13T10:00:00Z"));
        seed("HB-3", (short) 5, Instant.parse("2026-10-14T10:00:00Z"));

        List<OrderFeedbackDto> all = service.list(null);
        assertEquals(List.of("HB-3", "HB-2", "HB-1"),
                all.stream().map(OrderFeedbackDto::orderNumber).toList());

        List<OrderFeedbackDto> fives = service.list((short) 5);
        assertEquals(List.of("HB-3", "HB-1"),
                fives.stream().map(OrderFeedbackDto::orderNumber).toList());
    }

    @Test
    void deleteMissingThrowsNotFound() {
        assertThrows(NotFoundException.class, () -> service.delete(UUID.randomUUID()));
    }

    @Test
    void deleteExistingRemovesRow() {
        OrderFeedbackDto dto = service.create(
                new CreateOrderFeedbackRequest("HB-9", 4, null, null, null, null, null, null));

        service.delete(dto.id());

        assertTrue(service.list(null).isEmpty());
    }

    private void seed(String orderNumber, short rating, Instant createdAt) {
        OrderFeedback f = new OrderFeedback();
        f.setId(UUID.randomUUID());
        f.setOrderNumber(orderNumber);
        f.setRating(rating);
        f.setCreatedAt(createdAt);
        rows.put(f.getId(), f);
    }
}
