package com.vpnexues.svc.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vpnexues.svc.entity.AdminRole;
import com.vpnexues.svc.entity.AdminUser;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.repository.AdminUserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cover for {@code AdminUserManagementService.delete} — the only destructive admin endpoint.
 *
 * <p>Runs against the real local database but is wrapped in a transaction, so every row it
 * creates is rolled back; nothing real is ever removed.
 */
@SpringBootTest
@Transactional
class AdminUserManagementServiceDeleteTest {

    @Autowired private AdminUserManagementService adminUserManagementService;
    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AdminUser saveSubAdmin(String email) {
        AdminUser admin = new AdminUser();
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode("irrelevant-password-1"));
        admin.setName("Deletion Fixture");
        admin.setRole(AdminRole.SUB_ADMIN);
        admin.setCountryCode("IN");
        admin.setActive(true);
        admin.setAccessPassRequired(true);
        return adminUserRepository.save(admin);
    }

    private UUID unrelatedAdminId() {
        // The service only compares this against the target id (self-deletion guard), so any
        // id other than the row under test is a valid "someone else is asking" value.
        return UUID.randomUUID();
    }

    @Test
    void deletesSubAdminAndRevokesTheirSessions() {
        AdminUser doomed = saveSubAdmin("delete-fixture@vpnexues.test");
        UUID id = doomed.getId();

        adminUserManagementService.delete(id, unrelatedAdminId(), null);

        assertTrue(
                adminUserRepository.findById(id).isEmpty(),
                "the account row should be gone after a delete");
    }

    @Test
    void refusesToDeleteYourOwnAccount() {
        AdminUser self = saveSubAdmin("self-delete-fixture@vpnexues.test");

        BadRequestException thrown = assertThrows(
                BadRequestException.class,
                () -> adminUserManagementService.delete(self.getId(), self.getId(), null));

        assertTrue(thrown.getMessage().contains("your own account"), thrown.getMessage());
        assertTrue(
                adminUserRepository.findById(self.getId()).isPresent(),
                "a refused self-deletion must leave the account untouched");
    }

    @Test
    void refusesToDeleteSuperAdmin() {
        AdminUser superAdmin = saveSubAdmin("super-fixture@vpnexues.test");
        superAdmin.setRole(AdminRole.SUPER_ADMIN);
        adminUserRepository.save(superAdmin);

        BadRequestException thrown = assertThrows(
                BadRequestException.class,
                () -> adminUserManagementService.delete(superAdmin.getId(), unrelatedAdminId(), null));

        assertTrue(thrown.getMessage().contains("SUPER_ADMIN"), thrown.getMessage());
        assertTrue(adminUserRepository.findById(superAdmin.getId()).isPresent());
    }

    @Test
    void reportsMissingAccountAsNotFound() {
        UUID missing = UUID.randomUUID();
        assertFalse(adminUserRepository.findById(missing).isPresent());

        assertThrows(
                com.vpnexues.svc.exception.NotFoundException.class,
                () -> adminUserManagementService.delete(missing, unrelatedAdminId(), null));
    }
}
