package com.vpnexues.svc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A member of the public /team page; ordered by {@code displayOrder} (directors first). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "team_members")
public class TeamMember extends BaseEntity {

    /** URL-friendly unique key generated from the name on create; never changes on rename. */
    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    private String role;

    @Column(name = "photo_url", columnDefinition = "text")
    private String photoUrl;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "is_director", nullable = false)
    private boolean director;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /** Inactive members are hidden from the public /team page but kept for the admin list. */
    @Column(nullable = false)
    private boolean active = true;

    /** JSON array of {"title","description"} items for the detail page; null = role defaults. */
    @Column(columnDefinition = "text")
    private String responsibilities;

    /** JSON array of skill strings (Key Skills chips); null = role defaults. */
    @Column(name = "key_skills", columnDefinition = "text")
    private String keySkills;
}
