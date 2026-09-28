package com.vpnexues.svc.security;

import com.vpnexues.svc.service.CartOwner;
import com.vpnexues.svc.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class CartOwnerResolver {

    private static final Duration GUEST_COOKIE_MAX_AGE = Duration.ofDays(30);

    private final CartService cartService;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookies;

    public CartOwner resolve(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UUID userId) {
            adoptStrandedGuestCart(request, response, userId);
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

    /**
     * Folds a leftover guest cart into the logged-in customer's cart.
     *
     * <p>The login-time merge lives in {@code AuthController#verifyOtp} and only fires when the
     * {@code vpx_guest} cookie is attached to that one verify request. If it is missing there
     * (cross-site cookie timing, a restored {@code vpx_at} session, an interrupted login), the items
     * stay stranded in the guest cart while every later call — including checkout — reads the user
     * cart, which is empty. That is what surfaced as "Cannot checkout an empty cart" for a shopper
     * whose screen still showed a full cart. Adopting the guest cart on the first authenticated cart
     * call heals the split without relying on the verify request.
     */
    private void adoptStrandedGuestCart(HttpServletRequest request, HttpServletResponse response, UUID userId) {
        String strandedGuest = CookieUtil.read(request, CookieNames.GUEST_CART_TOKEN);
        if (strandedGuest == null) {
            return;
        }
        cartService.mergeGuestCartIntoUser(strandedGuest, userId);
        ResponseCookie clear = ResponseCookie.from(CookieNames.GUEST_CART_TOKEN, "")
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(secureCookies ? "None" : "Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, clear.toString());
    }
}
