package com.vpnexues.svc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Post-order customer feedback collected from the Order Success page's
 * feedback popup (rating 1-5 + improvement tags + free-form comment).
 * Anonymous-friendly: no user FK — guests can rate too; order_number is a
 * soft reference only.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_feedback")
public class OrderFeedback extends BaseEntity {

    @Column(name = "order_number", length = 30)
    private String orderNumber;

    @Column(nullable = false)
    private short rating;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "varchar(60)[]")
    private List<String> tags = new ArrayList<>();

    @Column(columnDefinition = "text")
    private String comment;

    @Column(length = 120)
    private String headline;

    @Column(name = "country_code", length = 4)
    private String countryCode;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;
}
