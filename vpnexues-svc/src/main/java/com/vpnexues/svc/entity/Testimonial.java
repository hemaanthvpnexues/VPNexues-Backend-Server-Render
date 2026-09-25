package com.vpnexues.svc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "testimonials")
public class Testimonial extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String role;

    private String location;

    // short, not int — the V2 migration column is SMALLINT and this migration is
    // already applied locally (Flyway-checksummed), so the Java side adapts instead
    // of editing an applied migration (see agents.md: migrations are append-only).
    @Column(nullable = false)
    private short rating;

    @Column(nullable = false, columnDefinition = "text")
    private String quote;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(nullable = false)
    private boolean active = true;
}
