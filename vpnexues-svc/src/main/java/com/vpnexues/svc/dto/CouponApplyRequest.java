package com.vpnexues.svc.dto;

import jakarta.validation.constraints.NotBlank;

public record CouponApplyRequest(@NotBlank String code) {
}
