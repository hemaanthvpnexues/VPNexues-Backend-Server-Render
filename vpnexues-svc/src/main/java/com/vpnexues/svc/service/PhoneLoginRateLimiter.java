package com.vpnexues.svc.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.vpnexues.svc.exception.TooManyRequestsException;
import com.vpnexues.svc.security.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Brute-force guard for the OTP-less phone login endpoint ({@code POST /api/auth/login}).
 * Phone-only login has no secret, so probing attempts are capped per phone number and
 * per client IP: {@value #MAX_ATTEMPTS_PER_PHONE} attempts per number and
 * {@value #MAX_ATTEMPTS_PER_IP} per IP inside a sliding {@value #WINDOW_MINUTES}-minute window.
 */
@Component
public class PhoneLoginRateLimiter {

    private static final int MAX_ATTEMPTS_PER_PHONE = 5;
    private static final int MAX_ATTEMPTS_PER_IP = 30;
    private static final long WINDOW_MINUTES = 5;
    private static final int MAX_KEYS = 10_000;

    private final Cache<String, AtomicInteger> phoneAttempts = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(WINDOW_MINUTES))
            .maximumSize(MAX_KEYS)
            .build();

    private final Cache<String, AtomicInteger> ipAttempts = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(WINDOW_MINUTES))
            .maximumSize(MAX_KEYS)
            .build();

    /**
     * Counts this attempt and throws {@link TooManyRequestsException} (HTTP 429)
     * once the phone or the client IP exceeds its allowance.
     */
    public void check(String phone, HttpServletRequest request) {
        boolean phoneOver = increment(phoneAttempts, phone) > MAX_ATTEMPTS_PER_PHONE;
        boolean ipOver = increment(ipAttempts, clientIp(request)) > MAX_ATTEMPTS_PER_IP;
        if (phoneOver || ipOver) {
            throw new TooManyRequestsException(
                    "Too many login attempts. Please try again in a few minutes.");
        }
    }

    private int increment(Cache<String, AtomicInteger> cache, String key) {
        return cache.get(key, k -> new AtomicInteger()).incrementAndGet();
    }

    private String clientIp(HttpServletRequest request) {
        return ClientIpUtil.from(request);
    }
}
