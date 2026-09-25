package com.vpnexues.svc.config;

import com.vpnexues.svc.entity.AdminRole;
import com.vpnexues.svc.entity.AdminUser;
import com.vpnexues.svc.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first SUPER_ADMIN from env vars on startup if admin_users is empty —
 * avoids ever needing a hand-computed bcrypt hash committed to a seed SQL file.
 * No-ops (with a log warning) if the bootstrap env vars aren't set; safe to leave
 * unset on every subsequent boot since it only acts when the table is empty.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.bootstrap-email:}")
    private String bootstrapEmail;

    @Value("${app.admin.bootstrap-password:}")
    private String bootstrapPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminUserRepository.count() > 0) {
            return;
        }
        if (bootstrapEmail.isBlank() || bootstrapPassword.isBlank()) {
            log.warn(
                    "No admin users exist and ADMIN_BOOTSTRAP_EMAIL/ADMIN_BOOTSTRAP_PASSWORD are not set — "
                            + "skipping bootstrap. Set both in .env to create the first SUPER_ADMIN.");
            return;
        }

        AdminUser admin = new AdminUser();
        admin.setEmail(bootstrapEmail);
        admin.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        admin.setName("Super Admin");
        admin.setRole(AdminRole.SUPER_ADMIN);
        admin.setMustChangePassword(true);
        adminUserRepository.save(admin);
        log.info("Bootstrapped initial SUPER_ADMIN account: {} (mustChangePassword=true)", bootstrapEmail);
    }
}
