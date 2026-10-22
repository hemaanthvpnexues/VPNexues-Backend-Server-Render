package com.vpnexues.svc.exception;

/**
 * Checkout rejected because the delivery address's city is outside VPNexues'
 * current coverage. Thrown BEFORE any order, payment or stock movement is
 * persisted, so nothing has to be undone. Carries the city (may be blank) so
 * the UI can show it on the "Coming Soon" page.
 */
public class DeliveryUnavailableException extends RuntimeException {

    private final String city;

    public DeliveryUnavailableException(String city, String message) {
        super(message);
        this.city = city;
    }

    public String getCity() {
        return city;
    }
}
