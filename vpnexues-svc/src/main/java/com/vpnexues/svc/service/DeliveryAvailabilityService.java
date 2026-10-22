package com.vpnexues.svc.service;

import com.vpnexues.svc.repository.ServiceableCityRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Delivery-city availability — the single source of truth for which cities
 * VPNexues delivers to, backed by the serviceable_cities table (V56 seed,
 * corrected by V52).
 *
 * <p>Coverage: Singapore = wildcard row (every city), India = Salem /
 * Chennai / Coimbatore only, US/AE/LK/MY = no rows (nothing yet).
 * The same rules gate {@code OrderService.checkout} (before any persistence)
 * and the public {@code GET /api/delivery/availability} probe, so the UI and
 * the API can never disagree.
 */
@Service
@RequiredArgsConstructor
public class DeliveryAvailabilityService {

    /** Row stored for a country where delivery covers EVERY city. */
    public static final String WILDCARD_CITY = "*";

    private final ServiceableCityRepository serviceableCityRepository;

    /**
     * Lookup key both sides of the comparison share: trim, collapse inner
     * whitespace, lowercase. Geocoders and customers vary casing/spacing
     * ("SALEM", "salem", " Salem "), none of which may change the outcome.
     */
    public static String normalize(String city) {
        if (city == null) return "";
        return city.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /**
     * True when VPNexues delivers to {@code city} in {@code countryCode}.
     * Deliberately false for blank countries/cities: an address whose city we
     * cannot identify must not be confirmed as deliverable.
     */
    @Transactional(readOnly = true)
    public boolean isServiceable(String countryCode, String city) {
        String country = countryCode == null ? "" : countryCode.trim().toUpperCase(Locale.ROOT);
        if (country.isEmpty()) return false;

        // Whole-country coverage first (Singapore): any city counts, even a
        // blank one the reverse-geocoder failed to fill in.
        if (serviceableCityRepository.existsByCountryCodeAndCityNameNormalizedAndActiveTrue(country, WILDCARD_CITY)) {
            return true;
        }

        String normalized = normalize(city);
        if (normalized.isEmpty()) return false;
        return serviceableCityRepository.existsByCountryCodeAndCityNameNormalizedAndActiveTrue(country, normalized);
    }
}
