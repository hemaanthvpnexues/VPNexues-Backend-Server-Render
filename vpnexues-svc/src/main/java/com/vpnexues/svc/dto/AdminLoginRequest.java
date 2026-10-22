package com.vpnexues.svc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Admin login payload.
 *
 * <p>{@code passcode} is Feature B's one-time access pass. It is optional at the DTO level
 * so the Super Admin (who is not gated) can sign in without one; delegated accounts
 * (ADMIN / SUB_ADMIN) are rejected by {@code AdminAuthController} when it is missing.
 */
public record AdminLoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @Size(max = 12) String passcode) {}
