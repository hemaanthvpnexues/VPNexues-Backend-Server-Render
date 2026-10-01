package com.vpnexues.svc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * OTP-less login: the phone number is the only credential, so this endpoint
 * relies on rate limiting (see PhoneLoginRateLimiter) instead of a secret.
 */
public record PhoneLoginRequest(
        @NotBlank @Pattern(regexp = "^[+0-9 ]{6,20}$") String phone) {
}
