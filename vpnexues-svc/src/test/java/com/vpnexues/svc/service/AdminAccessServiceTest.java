package com.vpnexues.svc.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vpnexues.svc.entity.AdminRole;
import com.vpnexues.svc.entity.AdminUser;
import com.vpnexues.svc.repository.AdminUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * Sub-admin module/permission enforcement (the Add/Edit Sub Admin checkboxes).
 *
 * Rules under test:
 *  - SUPER_ADMIN always bypasses.
 *  - NULL/blank modules = legacy account = full access (backward compatibility).
 *  - Reads require the module OR the "reports" module (analytics is read-only everywhere).
 *  - Mutations require the exact module AND the matching permission; blank permissions
 *    = full permission within granted modules (legacy behaviour).
 */
class AdminAccessServiceTest {

    private AdminUserRepository adminUserRepository;
    private AdminAccessService service;

    private UUID superAdminId;
    private UUID legacyAdminId;
    private UUID ordersOnlyId;
    private UUID reportsOnlyId;
    private UUID viewOnlyId;

    @BeforeEach
    void setUp() {
        adminUserRepository = mock(AdminUserRepository.class);
        service = new AdminAccessService(adminUserRepository);

        superAdminId = UUID.randomUUID();
        legacyAdminId = UUID.randomUUID();
        ordersOnlyId = UUID.randomUUID();
        reportsOnlyId = UUID.randomUUID();
        viewOnlyId = UUID.randomUUID();

        when(adminUserRepository.findById(superAdminId))
                .thenReturn(Optional.of(admin(superAdminId, AdminRole.SUPER_ADMIN, null)));
        when(adminUserRepository.findById(legacyAdminId))
                .thenReturn(Optional.of(admin(legacyAdminId, AdminRole.SUB_ADMIN, null)));
        when(adminUserRepository.findById(ordersOnlyId))
                .thenReturn(Optional.of(admin(ordersOnlyId, AdminRole.SUB_ADMIN, "orders", "view,edit")));
        when(adminUserRepository.findById(reportsOnlyId))
                .thenReturn(Optional.of(admin(reportsOnlyId, AdminRole.SUB_ADMIN, "reports", "view")));
        when(adminUserRepository.findById(viewOnlyId))
                .thenReturn(Optional.of(admin(viewOnlyId, AdminRole.SUB_ADMIN, "products,orders", "view")));
    }

    // ---------- bypass / legacy ----------

    @Test
    void superAdmin_bypassesEveryCheck() {
        assertDoesNotThrow(() -> service.requireRead(superAdminId, AdminAccessService.M_PRODUCTS));
        assertDoesNotThrow(
                () -> service.requirePermission(superAdminId, AdminAccessService.M_PRODUCTS, AdminAccessService.P_DELETE));
    }

    @Test
    void legacyAdmin_nullModules_keepsFullAccess() {
        assertDoesNotThrow(() -> service.requireRead(legacyAdminId, AdminAccessService.M_SUPPORT));
        assertDoesNotThrow(
                () -> service.requirePermission(legacyAdminId, AdminAccessService.M_CONTENT, AdminAccessService.P_DELETE));
    }

    // ---------- module reads ----------

    @Test
    void ordersAdmin_canReadOwnModule() {
        assertDoesNotThrow(() -> service.requireRead(ordersOnlyId, AdminAccessService.M_ORDERS));
    }

    @Test
    void ordersAdmin_cannotReadOtherModules() {
        assertThrows(AccessDeniedException.class,
                () -> service.requireRead(ordersOnlyId, AdminAccessService.M_PRODUCTS));
        assertThrows(AccessDeniedException.class,
                () -> service.requireRead(ordersOnlyId, AdminAccessService.M_USERS));
        assertThrows(AccessDeniedException.class,
                () -> service.requireRead(ordersOnlyId, AdminAccessService.M_SUPPORT));
    }

    @Test
    void reportsModule_impliesReadOnlyAccessEverywhere() {
        assertDoesNotThrow(() -> service.requireRead(reportsOnlyId, AdminAccessService.M_ORDERS));
        assertDoesNotThrow(() -> service.requireRead(reportsOnlyId, AdminAccessService.M_PAYMENTS));
        assertDoesNotThrow(
                () -> service.requireAnyRead(reportsOnlyId, AdminAccessService.M_PRODUCTS, AdminAccessService.M_CONTENT));
    }

    @Test
    void reportsModule_doesNotImplyMutations() {
        assertThrows(AccessDeniedException.class, () -> service.requirePermission(
                reportsOnlyId, AdminAccessService.M_ORDERS, AdminAccessService.P_EDIT));
    }

    // ---------- permissions ----------

    @Test
    void editPermission_allowsMutations() {
        assertDoesNotThrow(() -> service.requirePermission(
                ordersOnlyId, AdminAccessService.M_ORDERS, AdminAccessService.P_EDIT));
    }

    @Test
    void viewOnlyAdmin_cannotMutate() {
        assertThrows(AccessDeniedException.class, () -> service.requirePermission(
                viewOnlyId, AdminAccessService.M_ORDERS, AdminAccessService.P_EDIT));
        assertThrows(AccessDeniedException.class, () -> service.requirePermission(
                viewOnlyId, AdminAccessService.M_PRODUCTS, AdminAccessService.P_DELETE));
    }

    @Test
    void viewOnlyAdmin_canStillReadGrantedModules() {
        assertDoesNotThrow(() -> service.requireRead(viewOnlyId, AdminAccessService.M_PRODUCTS));
    }

    // ---------- edge cases ----------

    @Test
    void nullAdminId_isDenied() {
        assertThrows(AccessDeniedException.class, () -> service.requireRead(null, AdminAccessService.M_ORDERS));
    }

    @Test
    void splitCsv_parsesAndIgnoresBlank() {
        assertEquals(List.of("orders", "edit"), AdminAccessService.splitCsv("orders,edit"));
        assertEquals(List.of(), AdminAccessService.splitCsv(null));
        assertEquals(List.of(), AdminAccessService.splitCsv("  "));
    }

    // ---------- helpers ----------

    private AdminUser admin(UUID id, AdminRole role, String modules) {
        return admin(id, role, modules, null);
    }

    private AdminUser admin(UUID id, AdminRole role, String modules, String permissions) {
        AdminUser a = new AdminUser();
        a.setId(id);
        a.setRole(role);
        a.setModules(modules);
        a.setPermissions(permissions);
        return a;
    }
}
