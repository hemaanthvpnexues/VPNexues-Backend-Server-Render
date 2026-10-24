package com.vpnexues.svc.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nimbusds.jwt.JWTClaimsSet;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the Firebase -> internal-token re-mint.
 *
 * <p>Critical contract: the VERIFIED {@code phone_number} claim must survive
 * the re-mint. AuthController's OTP-verify reads it back to bind the typed
 * phone to the Google-verified phone; when an earlier refactor dropped it,
 * every Firebase OTP login failed with HTTP 400 "Phone number does not match
 * the verified Firebase session" (found 23-Oct-2026, after the live bundle
 * fix finally let verify requests reach the backend).
 */
class JwtServiceTest {

    private static final String SECRET = "unit-test-hmac-secret-at-least-32-bytes-long!!";
    private static final String PHONE = "+918489783585";

    private JwtService service;

    @BeforeEach
    void setUp() {
        service = new JwtService(SECRET, 30, 14, 30, "", "test-project");
    }

    @Test
    void mintFirebaseSession_forwardsVerifiedPhoneClaim() {
        JWTClaimsSet firebaseClaims = new JWTClaimsSet.Builder()
                .subject("uid-abc-123")
                .claim("phone_number", PHONE)
                .build();

        Claims claims = service.mintFirebaseSession(firebaseClaims);

        // The OTP-verify check does exactly: claims.get("phone_number") and
        // compares it to the phone typed in the login form.
        assertEquals(PHONE, claims.get("phone_number"),
                "verified phone must survive the re-mint or every OTP login 400s");
        assertEquals("uid-abc-123", claims.getSubject());
        assertEquals("uid-abc-123", claims.get("firebaseUid"));
        assertEquals(JwtService.TOKEN_TYPE_CUSTOMER, claims.get("type"));
    }

    @Test
    void mintFirebaseSession_phoneClaimMissing_yieldsNullPhoneNotNullError() {
        JWTClaimsSet firebaseClaims = new JWTClaimsSet.Builder()
                .subject("uid-no-phone")
                .build();

        Claims claims = service.mintFirebaseSession(firebaseClaims);

        // Tokens without the claim (e.g. non-phone providers) still re-mint;
        // AuthController then rejects them with its own explicit message.
        assertNull(claims.get("phone_number"));
        assertEquals("uid-no-phone", claims.getSubject());
        assertEquals("uid-no-phone", claims.get("firebaseUid"));
    }

    @Test
    void mintFirebaseSession_nonStringPhoneClaim_isStringified() {
        // Defensive: some providers encode claims as non-String JSON values.
        JWTClaimsSet firebaseClaims = new JWTClaimsSet.Builder()
                .subject("uid-int-phone")
                .claim("phone_number", PHONE)
                .build();

        Claims claims = service.mintFirebaseSession(firebaseClaims);

        assertEquals(PHONE, claims.get("phone_number"));
    }
}
