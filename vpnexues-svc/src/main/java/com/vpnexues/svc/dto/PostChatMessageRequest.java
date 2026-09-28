package com.vpnexues.svc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PostChatMessageRequest(
        @NotBlank @Pattern(regexp = "VISITOR|BOT|ADMIN") String sender,
        @NotBlank @Size(max = 2000) String body) {
}
