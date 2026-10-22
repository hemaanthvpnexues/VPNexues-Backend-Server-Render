package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.ServiceableCity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceableCityRepository extends JpaRepository<ServiceableCity, UUID> {

    /**
     * The single lookup the checkout gate uses. Pass {@code "*"} to test a
     * country's wildcard row (whole-country coverage), or a normalized city
     * name ("salem") for a specific city. Inactive rows never match.
     */
    boolean existsByCountryCodeAndCityNameNormalizedAndActiveTrue(String countryCode, String cityNameNormalized);
}
