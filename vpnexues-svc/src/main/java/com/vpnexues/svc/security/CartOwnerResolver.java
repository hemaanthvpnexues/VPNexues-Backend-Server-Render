package com.vpnexues.svc.security;

import com.vpnexues.svc.service.CartOwner;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves which cart a request belongs to: the authenticated customer's cart if logged
 * in, otherwise an opaque httpOnly guest-session cookie (never PII, just a random id
 * mapping to a server-side Cart row) — minted on first use if none exists yet.
 */
@Component
public class CartOwnerResolver {

    private static final Duration GUEST_COOKIE_MAX_AGE = Duration.ofDays(30);

    @Value("${app.cookie.secure:false}")
    private boolean secureCookies;

    public CartOwner resolve(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UUID userId) {
            return CartOwner.ofUser(userId);
        }

        String guestToken = CookieUtil.read(request, CookieNames.GUEST_CART_TOKEN);
        if (guestToken == null) {
            guestToken = UUID.randomUUID().toString();
            ResponseCookie cookie = ResponseCookie.from(CookieNames.GUEST_CART_TOKEN, guestToken)
                    .httpOnly(true)
                    .secure(secureCookies)
                    .sameSite(secureCookies ? "None" : "Lax")
                    .path("/")
                    .maxAge(GUEST_COOKIE_MAX_AGE)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return CartOwner.ofGuest(guestToken);
    }
}
