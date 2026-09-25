package com.vpnexues.svc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Per-country price override for a Product. Absent = no override for that country. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "product_price_overrides",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "country_code"}))
public class ProductPriceOverride extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** ISO-ish market code: IN, SG, US, AE. */
    @Column(name = "country_code", nullable = false, length = 4)
    private String countryCode;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(precision = 12, scale = 2)
    private BigDecimal oldPrice;
}
