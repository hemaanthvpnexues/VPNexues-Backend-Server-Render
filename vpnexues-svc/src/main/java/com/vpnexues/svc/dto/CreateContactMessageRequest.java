package com.vpnexues.svc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateContactMessageRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        String phone,
        String countryCode,
        @NotBlank String subject,
        @NotBlank String message) {
}
