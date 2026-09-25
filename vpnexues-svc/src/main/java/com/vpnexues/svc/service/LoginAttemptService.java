package com.vpnexues.svc.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * In-memory brute-force protection using Caffeine cache.
 *
 * IMPORTANT: This is JVM-local. If the application runs multiple backend instances,
 * login attempt counters are NOT shared between them. For horizontally scaled
 * production deployment, use Redis or a database-backed mechanism.
 */
@Slf4j
@Service
public class LoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration TRACKING_WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    /**
     * Key: email (lowercase). Value: AttemptTracker with count and lockout time.
     */
    private final Cache<String, AttemptTracker> attemptCache = Caffeine.newBuilder()
            .expireAfterWrite(TRACKING_WINDOW)
            .maximumSize(10_000)
            .build();

    /**
     * Returns true if the email is currently locked out (too many failed attempts).
     */
    public boolean isLockedOut(String email) {
        String key = normalize(email);
        AttemptTracker tracker = attemptCache.getIfPresent(key);
        if (tracker == null) {
            return false;
        }
        if (tracker.lockedUntil != null && tracker.lockedUntil.isAfter(Instant.now())) {
            return true;
        }
        return false;
    }

    /**
     * Returns remaining lockout seconds, or 0 if not locked.
     */
    public long getRemainingLockoutSeconds(String email) {
        String key = normalize(email);
        AttemptTracker tracker = attemptCache.getIfPresent(key);
        if (tracker == null || tracker.lockedUntil == null) {
            return 0;
        }
        long remaining = Instant.now().until(tracker.lockedUntil, ChronoUnit.SECONDS);
        return Math.max(0, remaining);
    }

    /**
     * Record a failed login attempt. Returns true if the account is now locked.
     */
    public boolean recordFailedAttempt(String email) {
        String key = normalize(email);
        AttemptTracker tracker = attemptCache.get(key, k -> new AttemptTracker());
        int count = tracker.failedCount.incrementAndGet();

        log.warn("Failed login attempt #{} for email={}", count, email);

        if (count >= MAX_FAILED_ATTEMPTS) {
            tracker.lockedUntil = Instant.now().plus(LOCKOUT_DURATION);
            log.warn("Account locked for {} seconds due to {} failed attempts, email={}",
                    LOCKOUT_DURATION.getSeconds(), count, email);
            return true;
        }
        return false;
    }

    /**
     * Clear all failed attempts after successful authentication.
     */
    public void clearAttempts(String email) {
        String key = normalize(email);
        attemptCache.invalidate(key);
    }

    private String normalize(String email) {
        return email == null ? "" : email.toLowerCase().trim();
    }

    private static class AttemptTracker {
        final AtomicInteger failedCount = new AtomicInteger(0);
        volatile Instant lockedUntil;
    }
}
