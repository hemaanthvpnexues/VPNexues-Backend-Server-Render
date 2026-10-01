package com.vpnexues.svc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateChatContactRequest(
        @NotBlank @Size(max = 100) String visitorName,
        @NotBlank @Email @Size(max = 255) String visitorEmail) {
}
