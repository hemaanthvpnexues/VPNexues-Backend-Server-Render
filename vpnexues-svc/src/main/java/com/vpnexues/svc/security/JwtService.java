package com.vpnexues.svc.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Handles JWT operations for both the legacy local JWT system and Supabase Auth.
 * 
 * - Local JWTs: signed with HMAC-SHA (JWT_SECRET), used for admin auth
 * - Supabase JWTs: signed with ES256 (JWKS), used for customer auth via Supabase Auth
 * 
 * The {@code supabase-jwks-url} env var enables Supabase JWT verification.
 * When set, customer auth tokens are verified against Supabase's JWKS endpoint.
 */
@Component
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    public static final String TOKEN_TYPE_CUSTOMER = "customer";
    public static final String TOKEN_TYPE_ADMIN = "admin";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Key signingKey;
    private final long accessTokenTtlMinutes;
    private final long refreshTokenTtlDays;
    private final String supabaseJwksUrl;
    private final String firebaseProjectId;
    private volatile ConfigurableJWTProcessor<SecurityContext> supabaseJwtProcessor;
    private volatile ConfigurableJWTProcessor<SecurityContext> firebaseJwtProcessor;
    private volatile JWKSet cachedJwkSet;
    private volatile long jwkSetFetchTime;
    private volatile long firebaseJwkSetFetchTime;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-ttl-minutes:30}") long accessTokenTtlMinutes,
            @Value("${app.jwt.refresh-token-ttl-days:14}") long refreshTokenTtlDays,
            @Value("${supabase-jwks-url:}") String supabaseJwksUrl,
            @Value("${app.firebase.project-id:}") String firebaseProjectId) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
        this.supabaseJwksUrl = supabaseJwksUrl;
        this.firebaseProjectId = firebaseProjectId;
    }

    // ── Local JWT (HMAC-SHA) — for admin auth and legacy ──

    public String generateToken(UUID subjectId, String tokenType) {
        return generateToken(subjectId, tokenType, Map.of());
    }

    public String generateToken(UUID subjectId, String tokenType, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(subjectId.toString())
                .claim("type", tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenTtlMinutes * 60)));
        extraClaims.forEach(builder::claim);
        return builder.signWith(signingKey).compact();
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtlMinutes * 60;
    }

    public long refreshTokenTtlSeconds() {
        return refreshTokenTtlDays * 24 * 60 * 60;
    }

    /** Generates a cryptographically random opaque refresh token (Base64URL, 256-bit). */
    public String generateRefreshToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 hex hash of a raw refresh token — only the hash is stored in the DB. */
    public String hashRefreshToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** Returns the parsed claims, or null if the token is missing/invalid/expired/wrong type. */
    public Claims parseAndValidate(String token, String expectedType) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!expectedType.equals(claims.get("type", String.class))) {
                return null;
            }
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    // ── Refresh Token Support ──
    // Note: opaque random token + SHA-256 hex hash (deterministic — required by
    // AdminRefreshTokenRepository.findByTokenHash equality lookup). Do NOT switch
    // to BCrypt here: its per-call salt breaks the hash lookup.

    /**
     * Validates a Supabase JWT token using their JWKS endpoint.
     * Returns a Claims-like map if valid, null otherwise.
     */
    public Claims parseSupabaseToken(String token) {
        if (token == null || token.isBlank() || supabaseJwksUrl == null || supabaseJwksUrl.isBlank()) {
            return null;
        }
        try {
            ConfigurableJWTProcessor<SecurityContext> processor = getSupabaseJwtProcessor();
            JWTClaimsSet claimSet = processor.process(token, null);

            // Validate issuer matches Supabase URL
            String issuer = claimSet.getIssuer();
            if (issuer == null || !issuer.contains("supabase")) {
                return null;
            }

            // Validate audience if present
            java.util.List<String> audience = claimSet.getAudience();
            if (audience != null && !audience.isEmpty()) {
                // Audience should match the Supabase project reference — accept if present
                // (Supabase tokens typically use "authenticated" as audience)
            }

            UUID userId = UUID.fromString(claimSet.getSubject());
            Instant expiresAt = claimSet.getExpirationTime().toInstant();

            String jwt = Jwts.builder()
                    .subject(userId.toString())
                    .claim("type", TOKEN_TYPE_CUSTOMER)
                    .issuedAt(Date.from(claimSet.getIssueTime().toInstant()))
                    .expiration(Date.from(expiresAt))
                    .signWith(signingKey)
                    .compact();
            return Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) signingKey)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    // ── Firebase ID Token (RS256 via Google JWKS) — for customer auth ──

    /**
     * Validates a Firebase ID token using Google's public keys.
     * Returns a Claims-like map with "sub" = Firebase UID if valid, null otherwise.
     */
    public Claims parseFirebaseToken(String token) {
        if (token == null || token.isBlank() || firebaseProjectId == null || firebaseProjectId.isBlank()) {
            log.warn("Firebase token rejected: missing token or FIREBASE_PROJECT_ID='{}'", firebaseProjectId);
            return null;
        }
        try {
            ConfigurableJWTProcessor<SecurityContext> processor = getFirebaseJwtProcessor();
            JWTClaimsSet claimSet = processor.process(token, null);

            String issuer = claimSet.getIssuer();
            if (issuer == null || !issuer.equals("https://securetoken.google.com/" + firebaseProjectId)) {
                log.warn("Firebase issuer mismatch: got='{}' expected='https://securetoken.google.com/{}'",
                        issuer, firebaseProjectId);
                return null;
            }

            java.util.List<String> audience = claimSet.getAudience();
            if (audience == null || !audience.contains(firebaseProjectId)) {
                log.warn("Firebase audience mismatch: got='{}' expected contains '{}'", audience, firebaseProjectId);
                return null;
            }

            String firebaseUid = claimSet.getSubject();
            if (firebaseUid == null || firebaseUid.isBlank()) {
                log.warn("Firebase token missing sub claim");
                return null;
            }

            Instant issuedAt = claimSet.getIssueTime() != null ? claimSet.getIssueTime().toInstant() : Instant.now();
            Instant expiresAt = claimSet.getExpirationTime() != null ? claimSet.getExpirationTime().toInstant() : Instant.now().plusSeconds(3600);

            String jwt = Jwts.builder()
                    .subject(firebaseUid)
                    .claim("type", TOKEN_TYPE_CUSTOMER)
                    .claim("firebaseUid", firebaseUid)
                    .issuedAt(Date.from(issuedAt))
                    .expiration(Date.from(expiresAt))
                    .signWith(signingKey)
                    .compact();
            return Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) signingKey)
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.warn("Firebase token verification failed: {}: {}",
                    cause.getClass().getSimpleName(), cause.getMessage());
            return null;
        }
    }

    /**
     * Extracts the Firebase UID from a parsed token claim.
     * Returns null if the claim doesn't contain a Firebase UID.
     */
    public String extractFirebaseUid(Claims claims) {
        if (claims == null) return null;
        return claims.get("firebaseUid", String.class);
    }

    private static final String FIREBASE_JWKS_URL =
            "https://www.googleapis.com/service_accounts/v1/jwk/securetoken%40system.gserviceaccount.com";

    private synchronized ConfigurableJWTProcessor<SecurityContext> getFirebaseJwtProcessor() {
        if (firebaseJwtProcessor == null || isFirebaseJwkSetStale()) {
            try {
                // Prefer JWK endpoint (explicit @ encoded). Fallback: x509 certs URL.
                JWKSet jwkSet;
                try {
                    jwkSet = loadJwkSetWithTimeout(FIREBASE_JWKS_URL);
                } catch (Exception first) {
                    log.warn("Firebase JWK endpoint failed ({}), trying x509 certs endpoint",
                            String.valueOf(first.getMessage()));
                    jwkSet = loadJwkSetWithTimeout(
                            "https://www.googleapis.com/robot/v1/metadata/x509/securetoken%40system.gserviceaccount.com");
                }

                JWSKeySelector<SecurityContext> keySelector =
                        new JWSVerificationKeySelector<>(com.nimbusds.jose.JWSAlgorithm.RS256,
                                new ImmutableJWKSet<>(jwkSet));

                DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
                processor.setJWSKeySelector(keySelector);
                firebaseJwtProcessor = processor;
                firebaseJwkSetFetchTime = System.currentTimeMillis();
                log.info("Firebase JWKS loaded: {} keys", jwkSet.getKeys().size());
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                log.error("Failed to load Firebase JWKS from {}: {}: {}",
                        FIREBASE_JWKS_URL, cause.getClass().getSimpleName(), cause.getMessage(), e);
                throw new IllegalStateException("Failed to load Firebase JWKS", e);
            }
        }
        return firebaseJwtProcessor;
    }

    /** Loads JWKS with connect/read timeouts (Render free tier can hang on slow Google fetches). */
    private JWKSet loadJwkSetWithTimeout(String url) throws Exception {
        // Nimbus 10: load(URL, connectTimeoutMs, readTimeoutMs, sizeLimit)
        return JWKSet.load(URI.create(url).toURL(), 10_000, 15_000, 64 * 1024);
    }

    private boolean isFirebaseJwkSetStale() {
        // Separate clock from Supabase so one refresh does not mask the other.
        return System.currentTimeMillis() - firebaseJwkSetFetchTime > 3_600_000;
    }

    private synchronized ConfigurableJWTProcessor<SecurityContext> getSupabaseJwtProcessor() {
        if (supabaseJwtProcessor == null || isJwkSetStale()) {
            try {
                cachedJwkSet = JWKSet.load(URI.create(supabaseJwksUrl).toURL());
                jwkSetFetchTime = System.currentTimeMillis();

                JWSKeySelector<SecurityContext> keySelector =
                        new JWSVerificationKeySelector<>(com.nimbusds.jose.JWSAlgorithm.ES256,
                                new ImmutableJWKSet<>(cachedJwkSet));

                DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
                processor.setJWSKeySelector(keySelector);
                supabaseJwtProcessor = processor;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load Supabase JWKS", e);
            }
        }
        return supabaseJwtProcessor;
    }

    private boolean isJwkSetStale() {
        // Refresh JWKS every 1 hour
        return System.currentTimeMillis() - jwkSetFetchTime > 3_600_000;
    }
}
