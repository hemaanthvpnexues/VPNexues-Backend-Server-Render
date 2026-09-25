package com.vpnexues.svc.service;

import java.util.UUID;

/** Exactly one of userId / guestToken is set — mirrors the chk_cart_owner DB constraint. */
public record CartOwner(UUID userId, String guestToken) {

    public static CartOwner ofUser(UUID userId) {
        return new CartOwner(userId, null);
    }

    public static CartOwner ofGuest(String guestToken) {
        return new CartOwner(null, guestToken);
    }

    public boolean isUser() {
        return userId != null;
    }
}
