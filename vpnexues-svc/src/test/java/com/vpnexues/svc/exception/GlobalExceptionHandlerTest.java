package com.vpnexues.svc.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Contract the frontend checkout relies on: a non-deliverable city must come
 * back as HTTP 409 with code=DELIVERY_NOT_AVAILABLE and the city, on the
 * standard success=false error envelope.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void deliveryUnavailableMapsTo409WithCodeAndCity() {
        ResponseEntity<Map<String, Object>> res =
                handler.handleDeliveryUnavailable(new DeliveryUnavailableException("Chennai", "Delivery is coming soon to Chennai"));

        assertEquals(409, res.getStatusCode().value());
        Map<String, Object> body = res.getBody();
        assertEquals(false, body.get("success"));
        assertEquals("DELIVERY_NOT_AVAILABLE", body.get("code"));
        assertEquals("Chennai", body.get("city"));
        assertEquals(409, body.get("status"));
        assertEquals("Delivery is coming soon to Chennai", body.get("message"));
    }

    @Test
    void deliveryUnavailableWithBlankCityStillCarriesTheCode() {
        ResponseEntity<Map<String, Object>> res =
                handler.handleDeliveryUnavailable(new DeliveryUnavailableException(null, "Delivery is coming soon to your area"));

        assertEquals(409, res.getStatusCode().value());
        assertEquals("DELIVERY_NOT_AVAILABLE", res.getBody().get("code"));
        assertEquals("", res.getBody().get("city"));
    }

    @Test
    void badRequestKeepsThePlainEnvelope() {
        ResponseEntity<Map<String, Object>> res = handler.handleBadRequest(new BadRequestException("Cannot checkout an empty cart"));

        assertEquals(400, res.getStatusCode().value());
        Map<String, Object> body = res.getBody();
        assertEquals(false, body.get("success"));
        assertEquals("Cannot checkout an empty cart", body.get("message"));
        // No delivery markers on generic errors — the UI must not misroute them.
        assertEquals(null, body.get("code"));
    }
}
