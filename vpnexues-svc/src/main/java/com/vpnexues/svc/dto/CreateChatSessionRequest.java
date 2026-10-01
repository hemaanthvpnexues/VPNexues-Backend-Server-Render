package com.vpnexues.svc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateChatSessionRequest(
        @NotBlank @Size(max = 100) String visitorName,
        @Email @Size(max = 255) String visitorEmail,
        @Size(max = 4) String countryCode,
        @Size(max = 200) String subject) {
}
