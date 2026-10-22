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
@Table(name = "career_applications")
public class CareerApplication extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(length = 255)
    private String location;

    @Column(nullable = false)
    private String role;

    @Column(length = 500)
    private String linkedin;

    @Column(length = 500)
    private String github;

    @Column(length = 1000)
    private String resume;

    @Column(length = 100)
    private String availability;

    @Column(length = 100)
    private String source;

    @Column(columnDefinition = "text")
    private String whyJoin;

    @Column(length = 100)
    private String timestamp;
}
