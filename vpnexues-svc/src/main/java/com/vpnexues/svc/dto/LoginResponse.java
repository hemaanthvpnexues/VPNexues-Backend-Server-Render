package com.vpnexues.svc.dto;

/**
 * Response of {@code POST /api/auth/login}.
 *
 * <p>Two outcomes:
 * <ul>
 *   <li>{@code requiresOtp=false} + {@code user} — the caller presented a valid
 *       trusted-device cookie ({@code vpx_dv}); the session cookie is already set.</li>
 *   <li>{@code requiresOtp=true} + {@code user=null} — registered number but a NEW
 *       device: no session is issued. The client must send a Firebase OTP and
 *       complete {@code POST /api/auth/otp/verify}, which then trusts this device.</li>
 * </ul>
 *
 * <p>Unknown numbers still get 404 so the UI can route them to signup.
 */
public record LoginResponse(boolean requiresOtp, UserDto user) {

    public static LoginResponse challenge() {
        return new LoginResponse(true, null);
    }

    public static LoginResponse session(UserDto user) {
        return new LoginResponse(false, user);
    }
}
