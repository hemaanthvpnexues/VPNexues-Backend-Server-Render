package com.vpnexues.svc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "admin_users")
public class AdminUser extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminRole role = AdminRole.ADMIN;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    /** Contact number collected on the Add/Edit Sub Admin form. */
    @Column(length = 20)
    private String phone;

    /**
     * Comma-separated module keys this admin may access (dashboard, orders, products,
     * users, reports, payments, support, content). NULL/blank = legacy account = full
     * access; see AdminAccessService.
     */
    @Column(length = 255)
    private String modules;

    /** Comma-separated action keys (view, edit, delete, export). NULL/blank = full permission within granted modules. */
    @Column(length = 255)
    private String permissions;

    @Column(name = "token_version", nullable = false)
    private int tokenVersion = 0;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    @Column(nullable = false)
    private boolean active = true;
}
