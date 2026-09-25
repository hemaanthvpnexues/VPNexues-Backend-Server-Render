package com.vpnexues.svc.dto;

import com.vpnexues.svc.entity.B2BEnquiryStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateB2BEnquiryStatusRequest(@NotNull B2BEnquiryStatus status) {
}
