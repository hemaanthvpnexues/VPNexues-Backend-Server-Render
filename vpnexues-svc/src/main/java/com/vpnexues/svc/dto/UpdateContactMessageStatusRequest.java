package com.vpnexues.svc.dto;

import com.vpnexues.svc.entity.ContactMessageStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateContactMessageStatusRequest(@NotNull ContactMessageStatus status) {
}
