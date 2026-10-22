package com.vpnexues.svc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vpnexues.svc.repository.ServiceableCityRepository;
import java.lang.reflect.Proxy;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Delivery-city availability rules — no DB, no Spring context. The in-memory
 * row set mirrors the seeded table (SG wildcard, IN salem/chennai/coimbatore
 * after the V52 correction, no rows for US/AE/LK/MY).
 */
class DeliveryAvailabilityServiceTest {

    /** Seeded rows as (country|normalized city) pairs, exactly as seeded. */
    private static final Set<String> SEEDED = Set.of(
            "SG|*",
            "IN|salem",
            "IN|chennai",
            "IN|coimbatore");

    private DeliveryAvailabilityService service;

    @BeforeEach
    void setUp() {
        ServiceableCityRepository repository = (ServiceableCityRepository) Proxy.newProxyInstance(
                ServiceableCityRepository.class.getClassLoader(),
                new Class<?>[] { ServiceableCityRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "existsByCountryCodeAndCityNameNormalizedAndActiveTrue" ->
                            SEEDED.contains(args[0] + "|" + args[1]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        service = new DeliveryAvailabilityService(repository);
    }

    @Test
    void singaporeIsServiceableForEveryCity() {
        // Wildcard row: any city (even a blank one) passes in Singapore.
        assertTrue(service.isServiceable("SG", "Singapore"));
        assertTrue(service.isServiceable("SG", "Jurong"));
        assertTrue(service.isServiceable("SG", ""));
        assertTrue(service.isServiceable("sg", "Woodlands"));
    }

    @Test
    void indianServiceableCitiesMatchRegardlessOfCasingAndSpacing() {
        assertTrue(service.isServiceable("IN", "Salem"));
        assertTrue(service.isServiceable("IN", "salem"));
        assertTrue(service.isServiceable("IN", "SALEM"));
        assertTrue(service.isServiceable("IN", "  salem  "));
        assertTrue(service.isServiceable("IN", "Chennai"));
        assertTrue(service.isServiceable("IN", "chennai"));
        assertTrue(service.isServiceable("IN", "  CHENNAI  "));
        assertTrue(service.isServiceable("IN", "Coimbatore"));
        assertTrue(service.isServiceable("IN", "COIMBATORE"));
    }

    @Test
    void otherIndianCitiesAreNotServiceable() {
        assertFalse(service.isServiceable("IN", "Bengaluru"));
        assertFalse(service.isServiceable("IN", "Mumbai"));
        assertFalse(service.isServiceable("IN", "Tirupattur"));
        assertFalse(service.isServiceable("IN", "Madurai"));
    }

    @Test
    void supportedButNotYetDeliveringCountriesAreNotServiceable() {
        assertFalse(service.isServiceable("US", "Austin"));
        assertFalse(service.isServiceable("AE", "Dubai"));
        assertFalse(service.isServiceable("LK", "Colombo"));
        assertFalse(service.isServiceable("MY", "Kuala Lumpur"));
    }

    @Test
    void blankCityOrCountryIsNeverServiceable() {
        // India has no wildcard row, so an unidentifiable city must not pass.
        assertFalse(service.isServiceable("IN", ""));
        assertFalse(service.isServiceable("IN", "   "));
        assertFalse(service.isServiceable("IN", null));
        assertFalse(service.isServiceable(null, "Salem"));
        assertFalse(service.isServiceable("", "Salem"));
        assertFalse(service.isServiceable("  ", "Salem"));
    }

    @Test
    void normalizeCollapsesWhitespaceAndCase() {
        assertEquals("salem", DeliveryAvailabilityService.normalize(" SALEM "));
        assertEquals("new york", DeliveryAvailabilityService.normalize("New   York"));
        assertEquals("", DeliveryAvailabilityService.normalize(null));
        assertEquals("", DeliveryAvailabilityService.normalize("   "));
    }
}
