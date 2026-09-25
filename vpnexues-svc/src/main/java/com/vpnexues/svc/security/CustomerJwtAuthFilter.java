package com.vpnexues.svc.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Customer auth filter — checks in order:
 * 1. httpOnly cookie (backend JWT) — primary after login
 * 2. Bearer token (Firebase ID token) — used only during /api/auth/otp/verify
 * 3. Bearer token (Supabase JWT) — backward compatibility
 */
@RequiredArgsConstructor
public class CustomerJwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CustomerJwtAuthFilter.class);
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Claims claims = null;

        // 1. Try httpOnly cookie first (backend JWT — has UUID subject)
        String cookieToken = CookieUtil.read(request, CookieNames.CUSTOMER_ACCESS_TOKEN);
        claims = jwtService.parseAndValidate(cookieToken, JwtService.TOKEN_TYPE_CUSTOMER);

        // 2. Fall back to Bearer token (Firebase or Supabase)
        if (claims == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                // Try Firebase first, then Supabase
                claims = jwtService.parseFirebaseToken(token);
                if (claims == null) {
                    claims = jwtService.parseSupabaseToken(token);
                }
            }
        }

        if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String subject = claims.getSubject();
            UUID userId;
            try {
                userId = UUID.fromString(subject);
            } catch (IllegalArgumentException e) {
                // Subject is not a UUID (e.g., Firebase UID) — pass through without auth
                // This happens during /api/auth/otp/verify where the controller handles user lookup
                log.debug("Non-UUID subject in token: {} — passing through", subject);
                filterChain.doFilter(request, response);
                return;
            }
            var auth = new UsernamePasswordAuthenticationToken(
                    userId, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
