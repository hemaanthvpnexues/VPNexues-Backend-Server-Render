package com.vpnexues.svc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record CreateB2BEnquiryRequest(
        @NotBlank String companyName,
        @NotBlank String contactName,
        @NotBlank @Email String email,
        String phone,
        @NotBlank String countryCode,
        String productInterest,
        String quantity,
        BigDecimal estimatedValue) {
}
