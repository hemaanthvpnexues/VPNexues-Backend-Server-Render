package com.vpnexues.svc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row per serviceable city (seeded by V56__serviceable_cities.sql).
 * The country-wide wildcard row is {@code city_name = '*'} — a country that
 * delivers everywhere (Singapore) needs no per-city rows. Countries with no
 * rows at all (US, AE, LK, MY) deliver nowhere yet.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "serviceable_cities")
public class ServiceableCity extends BaseEntity {

    /** ISO country code, uppercase — matches orders.country_code. */
    @Column(name = "country_code", nullable = false, length = 4)
    private String countryCode;

    /** Display spelling as seeded (e.g. "Salem"). */
    @Column(name = "city_name", nullable = false, length = 120)
    private String cityName;

    /** Lookup key: lowercased + whitespace-collapsed (see DeliveryAvailabilityService). */
    @Column(name = "city_name_normalized", nullable = false, length = 120)
    private String cityNameNormalized;

    @Column(nullable = false)
    private boolean active = true;
}
