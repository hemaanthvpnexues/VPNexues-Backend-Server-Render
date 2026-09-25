package com.vpnexues.svc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailCodeRequest(
        @NotBlank @Email String email, @NotBlank @Pattern(regexp = "^[0-9]{4,6}$") String code) {
}
