package com.vpnexues.svc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cart_items")
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    /** Null for synthetic Small/Big Box line items (boxType set instead). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    /** SMALL_BOX / BIG_BOX — set only when product is null. */
    private String boxType;

    @Column(nullable = false)
    private Integer qty;

    /** Unit price snapshot at the moment the item was added, in the cart's country currency. */
    @Column(name = "unit_price_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceSnapshot;

    /** Product's old/MRP price at the moment the item was added; null if the product has no old price. */
    @Column(name = "old_price_snapshot", precision = 12, scale = 2)
    private BigDecimal oldPriceSnapshot;

    /** BIG_BOX only — weight in kg (0.5 increments), priced as unitPriceSnapshot per kg. Null otherwise. */
    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;
}
