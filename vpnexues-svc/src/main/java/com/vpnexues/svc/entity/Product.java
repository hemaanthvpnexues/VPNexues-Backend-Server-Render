package com.vpnexues.svc.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products")
// Cart mutations walk every cart line's lazy product proxy (price re-resolve, box grouping). Batching their
// loads keeps that to one IN (...) query instead of one round trip per item - at ~150-200 ms per round trip
// (Oregon app -> Mumbai database) that difference alone was seconds.
@BatchSize(size = 50)
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String category;

    @Column(columnDefinition = "text")
    private String description;

    /** Base price in SGD, converted per country unless a ProductPriceOverride exists. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(precision = 12, scale = 2)
    private BigDecimal oldPrice;

    @Column(nullable = false)
    private String unit;

    @Column(nullable = false, unique = true)
    private String sku;

    private String imageUrl;

    private Integer discountPct;

    @ElementCollection
    @CollectionTable(name = "product_badges", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "badge")
    private List<String> badges = new ArrayList<>();

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "sales_count", nullable = false)
    private int salesCount = 0;

    @Column(name = "sales_count_sg", nullable = false)
    private int salesCountSg = 0;

    @Column(name = "sales_count_us", nullable = false)
    private int salesCountUs = 0;

    @Column(name = "sales_count_ae", nullable = false)
    private int salesCountAe = 0;
}
