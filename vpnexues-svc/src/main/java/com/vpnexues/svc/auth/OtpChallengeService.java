package com.vpnexues.svc.auth;

import com.vpnexues.svc.entity.OtpChallenge;
import com.vpnexues.svc.repository.OtpChallengeRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared, database-backed OTP challenge store used by every {@link OtpProvider} (stub, WhatsApp, ...)
 * and by email verification — one code path instead of each channel keeping its own in-memory map, and
 * codes survive app restarts / work correctly across multiple backend instances.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OtpChallengeService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TTL_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int SEND_COOLDOWN_SECONDS = 60;

    private final OtpChallengeRepository otpChallengeRepository;

    /** Generates a fresh 6-digit code for (channel, identifier), overwriting any existing pending challenge. */
    public String generateAndStore(String channel, String identifier) {
        OtpChallenge existing = otpChallengeRepository
                .findByChannelAndIdentifier(channel, identifier)
                .orElse(null);

        // Rate limit: prevent rapid re-sends within cooldown window
        if (existing != null && existing.getExpiresAt() != null) {
            Instant cooldownEnd = existing.getUpdatedAt() != null
                    ? existing.getUpdatedAt().plus(SEND_COOLDOWN_SECONDS, ChronoUnit.SECONDS)
                    : existing.getExpiresAt();
            if (Instant.now().isBefore(cooldownEnd) && existing.getAttemptsRemaining() < MAX_ATTEMPTS) {
                // Code was already sent recently and not fully exhausted — return existing code
                return existing.getCode();
            }
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        OtpChallenge challenge = existing != null ? existing : new OtpChallenge();
        challenge.setChannel(channel);
        challenge.setIdentifier(identifier);
        challenge.setCode(code);
        challenge.setExpiresAt(Instant.now().plus(TTL_MINUTES, ChronoUnit.MINUTES));
        challenge.setAttemptsRemaining(MAX_ATTEMPTS);
        otpChallengeRepository.save(challenge);
        return code;
    }

    /** Verifies a code for (channel, identifier); deletes the challenge on success, decrements attempts on mismatch. */
    public boolean verify(String channel, String identifier, String code) {
        OtpChallenge challenge = otpChallengeRepository
                .findByChannelAndIdentifier(channel, identifier)
                .orElse(null);
        if (challenge == null || challenge.getExpiresAt().isBefore(Instant.now())) {
            return false;
        }
        if (challenge.getAttemptsRemaining() <= 0) {
            otpChallengeRepository.delete(challenge);
            return false;
        }
        boolean matches = challenge.getCode().equals(code);
        if (matches) {
            otpChallengeRepository.delete(challenge);
        } else {
            challenge.setAttemptsRemaining(challenge.getAttemptsRemaining() - 1);
            otpChallengeRepository.save(challenge);
        }
        return matches;
    }
}
