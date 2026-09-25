package com.vpnexues.svc.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record B2BEnquiryDto(
        UUID id,
        String companyName,
        String contactName,
        String email,
        String phone,
        String countryCode,
        String productInterest,
        String quantity,
        BigDecimal estimatedValue,
        String status,
        Instant createdAt) {
}
