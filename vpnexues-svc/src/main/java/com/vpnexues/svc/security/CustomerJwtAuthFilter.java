package com.vpnexues.svc.security;

import com.vpnexues.svc.entity.User;
import com.vpnexues.svc.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Customer auth filter — checks in order:
 * 1. httpOnly cookie (backend JWT) — primary after login, long-lived + sliding renewal
 * 2. Bearer token (Firebase ID token) — survives browsers that block third-party cookies
 * 3. Bearer token (Supabase JWT) — backward compatibility
 *
 * A customer must stay signed in across a page reload, otherwise /api/auth/me 401s (user vanishes)
 * and CartOwnerResolver falls back to a brand-new empty guest cart (cart items vanish). Both of the
 * two independent signals above therefore resolve to the same user id.
 */
@RequiredArgsConstructor
public class CustomerJwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CustomerJwtAuthFilter.class);
    /** How long a firebaseUid -> userId mapping may be reused (avoids a query on every request). */
    private static final long FIREBASE_LOOKUP_TTL_MILLIS = 60_000L;
    private static final int FIREBASE_LOOKUP_CACHE_MAX = 1_000;

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final boolean secureCookies;

    private final ConcurrentMap<String, Lookup> firebaseUserCache = new ConcurrentHashMap<>();

    private record Lookup(UUID userId, long fetchedAt) {
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Claims claims = null;
        boolean fromCookie = false;
        boolean fromFirebase = false;

        // 1. Try httpOnly cookie first (backend JWT — has UUID subject)
        String cookieToken = CookieUtil.read(request, CookieNames.CUSTOMER_ACCESS_TOKEN);
        claims = jwtService.parseAndValidate(cookieToken, JwtService.TOKEN_TYPE_CUSTOMER);
        fromCookie = claims != null;

        // 2. Fall back to Bearer token (Firebase or Supabase)
        if (claims == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                // Try Firebase first, then Supabase
                claims = jwtService.parseFirebaseToken(token);
                fromFirebase = claims != null;
                if (claims == null) {
                    claims = jwtService.parseSupabaseToken(token);
                }
            }
        }

        if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String subject = claims.getSubject();
            UUID userId = tryParseUuid(subject);

            if (userId == null && fromFirebase) {
                // Firebase UID (not a UUID): resolve it to our user row so the session survives
                // even when the cross-site cookie is unavailable.
                userId = resolveUserByFirebaseUid(subject);
            }

            if (userId == null) {
                // Subject is not resolvable (e.g. Supabase JWT) — pass through without auth.
                // This happens during /api/auth/otp/verify where the controller handles user lookup.
                log.debug("Non-UUID subject in token: {} — passing through", subject);
                filterChain.doFilter(request, response);
                return;
            }

            var auth = new UsernamePasswordAuthenticationToken(
                    userId, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
            SecurityContextHolder.getContext().setAuthentication(auth);

            if (fromCookie) {
                renewCookieIfAging(response, claims, userId);
            }
        }

        filterChain.doFilter(request, response);
    }

    private static UUID tryParseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private UUID resolveUserByFirebaseUid(String firebaseUid) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            return null;
        }
        long now = System.currentTimeMillis();
        Lookup cached = firebaseUserCache.get(firebaseUid);
        if (cached != null && now - cached.fetchedAt() < FIREBASE_LOOKUP_TTL_MILLIS) {
            return NULL_UUID.equals(cached.userId()) ? null : cached.userId();
        }
        UUID userId = userRepository.findByFirebaseUid(firebaseUid).map(User::getId).orElse(null);
        if (firebaseUserCache.size() >= FIREBASE_LOOKUP_CACHE_MAX) {
            firebaseUserCache.clear();
        }
        firebaseUserCache.put(firebaseUid, new Lookup(userId == null ? NULL_UUID : userId, now));
        return userId;
    }

    /** Sentinel so "no such user" is not re-queried on every single request. */
    private static final UUID NULL_UUID = new UUID(0L, 0L);

    /**
     * Sliding renewal: once a customer session is more than half spent, re-issue the cookie so an
     * active shopper is never logged out mid-session. Cheap — fires at most once per half TTL.
     */
    private void renewCookieIfAging(HttpServletResponse response, Claims claims, UUID userId) {
        Date expiresAt = claims.getExpiration();
        if (expiresAt == null) {
            return;
        }
        long ttlSeconds = jwtService.customerAccessTokenTtlSeconds();
        long remainingMillis = expiresAt.getTime() - System.currentTimeMillis();
        if (remainingMillis > (ttlSeconds * 1000L) / 2) {
            return; // still fresh
        }
        ResponseCookie cookie = ResponseCookie.from(
                        CookieNames.CUSTOMER_ACCESS_TOKEN, jwtService.generateCustomerToken(userId))
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(secureCookies ? "None" : "Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(ttlSeconds))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
