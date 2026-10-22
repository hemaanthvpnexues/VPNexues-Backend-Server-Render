package com.vpnexues.svc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A public careers job listing, managed from the sub-admin dashboard.
 *
 * <p>Stored detail lists are JSON-in-TEXT, exactly like {@code team_members} (V37):
 * {@code responsibilities}/{@code requirements}/{@code perks} hold a JSON array of
 * strings, {@code openings} a JSON array of {@code {role, type, responsibilities[]}}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "career_jobs")
public class CareerJob extends BaseEntity {

    /**
     * Stable public key: it is what /careers/apply?job=&lt;slug&gt; links use and what the
     * 5-language job translations are keyed on, so it is generated once on create and
     * never changes on rename (same rule as team_members.slug).
     */
    @Column(nullable = false, length = 100)
    private String slug;

    /** Font Awesome class, e.g. {@code fa-solid fa-laptop-code}. */
    @Column(length = 100)
    private String icon;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false, columnDefinition = "text")
    private String skills;

    @Column(length = 100)
    private String duration;

    @Column(length = 100)
    private String stipend;

    @Column(length = 100, name = "start_date")
    private String startDate;

    @Column(columnDefinition = "text")
    private String about;

    /** JSON array of strings. */
    @Column(columnDefinition = "text")
    private String responsibilities;

    /** JSON array of strings. */
    @Column(columnDefinition = "text")
    private String requirements;

    /** JSON array of strings. */
    @Column(columnDefinition = "text")
    private String perks;

    /** JSON array of {@code {role, type, responsibilities[]}} — null means a single-role job. */
    @Column(columnDefinition = "text")
    private String openings;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
