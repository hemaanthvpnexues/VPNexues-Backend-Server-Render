package com.vpnexues.svc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateContactMessageRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 20) String phone,
        @Size(max = 4) String countryCode,
        @NotBlank @Size(max = 255) String subject,
        @NotBlank @Size(max = 10000) String message) {
}
