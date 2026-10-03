package com.vpnexues.svc.security;

public final class CookieNames {
    public static final String CUSTOMER_ACCESS_TOKEN = "vpx_at";
    public static final String ADMIN_ACCESS_TOKEN = "vpx_admin_at";
    public static final String ADMIN_REFRESH_TOKEN = "vpx_admin_rt";
    public static final String GUEST_CART_TOKEN = "vpx_guest";
    /** Long-lived trusted-device proof (hashed server-side) — required for OTP-less phone login. */
    public static final String TRUSTED_DEVICE = "vpx_dv";

    private CookieNames() {
    }
}
