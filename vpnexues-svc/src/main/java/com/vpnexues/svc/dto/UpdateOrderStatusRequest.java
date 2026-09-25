package com.vpnexues.svc.dto;

import com.vpnexues.svc.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
}
