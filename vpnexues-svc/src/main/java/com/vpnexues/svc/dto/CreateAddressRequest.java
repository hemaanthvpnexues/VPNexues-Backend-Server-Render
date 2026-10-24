package com.vpnexues.svc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Length caps mirror the {@code user_addresses} column limits in
 * V1__initial_schema.sql (label 50, name 255, phone 20, city 100, pincode 20,
 * state 100). Without these a geocoder-supplied value longer than its column
 * sailed past bean validation and died as an unhandled 500 in Postgres
 * ("value too long for type character varying(n)") - which is exactly why
 * saving a Karnataka address returned 500 instead of a readable message.
 *
 * <p>{@code city}/{@code pincode}/{@code state} stay optional (blank is a legal
 * value the service normalises to ""), but when present they are capped.
 */
public record CreateAddressRequest(
        @NotBlank @Size(max = 50) String label,
        @NotBlank @Size(max = 255) String name,
        // Format rule left as-is (spaces allowed) so editing a legacy address whose
        // phone was stored as "+91 99439 94603" keeps working; only the LENGTH cap
        // is new, and that is what actually overflowed VARCHAR(20).
        @NotBlank @Size(max = 20) @Pattern(regexp = "^[+0-9 ]{6,20}$") String phone,
        @NotBlank @Size(max = 2000) String addressLine,
        @Size(max = 255) String flat,
        @Size(max = 255) String landmark,
        @Size(max = 100) String city,
        @Size(max = 20) String pincode,
        @Size(max = 100) String state,
        // V59: ISO-3166 alpha-2 country of this address. Optional (legacy rows and
        // failed reverse-geocodes omit it), capped at 2 to match the column.
        @Size(max = 2) String country,
        boolean isDefault,
        Double lat,
        Double lng) {
}
