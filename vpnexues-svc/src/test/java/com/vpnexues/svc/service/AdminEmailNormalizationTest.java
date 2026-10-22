package com.vpnexues.svc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vpnexues.svc.entity.AdminUser;
import com.vpnexues.svc.repository.AdminUserRepository;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regression cover for the mixed-case admin email defect fixed by V52.
 *
 * <p>Login lower-cases the submitted address before looking it up
 * ({@code AdminAuthController}), but Add Sub Admin used to persist the address
 * exactly as typed. {@code admin_users.email} is a plain {@code VARCHAR UNIQUE},
 * so the derived lookup was case-sensitive and a capitalised address could never
 * be found again — the request died with "Invalid email or password" at the
 * email-lookup step, two steps before the Feature B access-pass gate. That is
 * why a perfectly correct one-time pass appeared to be the problem when it was not.
 *
 * <p>These tests run against the real local database, so they also confirm that
 * the V52 data migration has actually been applied.
 */
@SpringBootTest
@Transactional
class AdminEmailNormalizationTest {

    @Autowired private AdminUserRepository adminUserRepository;

    /** V52 lower-cased every stored address; nothing may drift back. */
    @Test
    void storedEmailsAreLowercase() {
        List<String> drifted = adminUserRepository.findAll().stream()
                .map(AdminUser::getEmail)
                .filter(email -> email != null && !email.equals(email.toLowerCase()))
                .toList();

        assertTrue(
                drifted.isEmpty(),
                "admin_users rows still holding a mixed-case email — V52 has not applied to this "
                        + "database: "
                        + drifted);
    }

    /** The lookup must resolve an address however it is cased on either side. */
    @Test
    void findByEmailIsCaseInsensitive() {
        AdminUser admin = adminUserRepository.findAll().stream().findFirst().orElse(null);
        Assumptions.assumeTrue(admin != null, "no admin rows to probe");

        String stored = admin.getEmail();

        assertTrue(adminUserRepository.findByEmail(stored.toLowerCase()).isPresent());
        assertTrue(adminUserRepository.findByEmail(stored.toUpperCase()).isPresent());
        assertEquals(
                admin.getId(),
                adminUserRepository.findByEmail(stored.toUpperCase()).orElseThrow().getId());
    }
}
