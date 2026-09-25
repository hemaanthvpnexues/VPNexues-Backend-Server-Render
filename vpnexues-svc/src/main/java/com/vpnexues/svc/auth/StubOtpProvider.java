package com.vpnexues.svc.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Free, no-account-needed OTP "delivery" for local dev/testing: generates a real 6-digit code (via
 * the shared {@link OtpChallengeService}) and logs it instead of sending an SMS/WhatsApp message.
 * Never use this outside local/trusted environments — anyone who can read the server log can read the OTP.
 */
@Slf4j
@RequiredArgsConstructor
public class StubOtpProvider implements OtpProvider {

    private static final String CHANNEL = "PHONE";

    private final OtpChallengeService otpChallengeService;

    @Override
    public void send(String phone) {
        String code = otpChallengeService.generateAndStore(CHANNEL, phone);
        log.info("[STUB OTP] phone={} code={}", phone, code);
    }

    @Override
    public boolean verify(String phone, String code) {
        return otpChallengeService.verify(CHANNEL, phone, code);
    }
}
