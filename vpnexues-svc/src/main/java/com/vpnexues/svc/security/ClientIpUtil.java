package com.vpnexues.svc.security;

import jakarta.servlet.http.HttpServletRequest;

/** Resolves the real client IP behind Render/GoDaddy proxies (X-Forwarded-For). */
public final class ClientIpUtil {

    private ClientIpUtil() {
    }

    public static String from(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
