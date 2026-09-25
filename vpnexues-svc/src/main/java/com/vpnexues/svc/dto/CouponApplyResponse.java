package com.vpnexues.svc.dto;

import java.math.BigDecimal;

public record CouponApplyResponse(boolean valid, String code, BigDecimal discountAmount, String message) {
}
