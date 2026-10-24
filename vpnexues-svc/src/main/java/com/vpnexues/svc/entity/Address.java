package com.vpnexues.svc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_addresses")
public class Address extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Home / Work / Other */
    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Column(name = "address_line", nullable = false, columnDefinition = "text")
    private String addressLine;

    private String flat;

    private String landmark;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String pincode;

    @Column(nullable = false)
    private String state;

    /**
     * ISO-3166 alpha-2 country this address is IN ("IN", "SG", ...), populated
     * by the reverse geocoder when the address is saved.
     *
     * <p>Backed by V59. Nullable so rows saved before that migration (and rows
     * whose geocoder returned nothing) still save; checkout falls back to the
     * selected country when this is blank. This column is what stops a
     * Singapore-selected header from validating an Indian city against
     * Singapore's wildcard coverage.
     */
    @Column(length = 2)
    private String country;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    private Double lat;

    private Double lng;
}
