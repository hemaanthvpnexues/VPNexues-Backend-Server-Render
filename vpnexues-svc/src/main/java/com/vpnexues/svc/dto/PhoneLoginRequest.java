package com.vpnexues.svc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Phone login. The phone number alone never authorizes a session anymore:
 * the backend ALSO requires a valid trusted-device cookie ({@code vpx_dv}) from a
 * browser that already passed OTP — otherwise it answers {@code requiresOtp:true}
 * and the client must run the Firebase OTP flow. Rate limiting
 * (see {@code PhoneLoginRateLimiter}) still caps probing on this endpoint.
 */
public record PhoneLoginRequest(
        @NotBlank @Pattern(regexp = "^[+0-9 ]{6,20}$") String phone) {
}
