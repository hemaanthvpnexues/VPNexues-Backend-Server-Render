package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.CreateOrderFeedbackRequest;
import com.vpnexues.svc.dto.OrderFeedbackDto;
import com.vpnexues.svc.entity.OrderFeedback;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.OrderFeedbackRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderFeedbackService {

    private final OrderFeedbackRepository orderFeedbackRepository;

    public OrderFeedbackDto create(CreateOrderFeedbackRequest req) {
        OrderFeedback entity = new OrderFeedback();
        entity.setOrderNumber(trimToNull(req.orderNumber()));
        entity.setRating((short) req.rating());
        entity.setTags(req.tags() == null
                ? List.of()
                : req.tags().stream()
                        .filter(t -> t != null && !t.isBlank())
                        .map(String::trim)
                        .distinct()
                        .collect(Collectors.toList()));
        entity.setComment(trimToNull(req.comment()));
        entity.setHeadline(trimToNull(req.headline()));
        entity.setCountryCode(trimToNull(req.countryCode()));
        entity.setContactEmail(trimToNull(req.contactEmail()));
        entity.setContactPhone(trimToNull(req.contactPhone()));
        return toDto(orderFeedbackRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<OrderFeedbackDto> list(Short rating) {
        List<OrderFeedback> rows = rating == null
                ? orderFeedbackRepository.findAllByOrderByCreatedAtDesc()
                : orderFeedbackRepository.findByRatingOrderByCreatedAtDesc(rating);
        return rows.stream().map(this::toDto).toList();
    }

    public void delete(UUID id) {
        if (!orderFeedbackRepository.existsById(id)) {
            throw new NotFoundException("Order feedback not found: " + id);
        }
        orderFeedbackRepository.deleteById(id);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private OrderFeedbackDto toDto(OrderFeedback f) {
        return new OrderFeedbackDto(
                f.getId(),
                f.getOrderNumber(),
                f.getRating(),
                f.getTags(),
                f.getComment(),
                f.getHeadline(),
                f.getCountryCode(),
                f.getContactEmail(),
                f.getContactPhone(),
                f.getCreatedAt());
    }
}
