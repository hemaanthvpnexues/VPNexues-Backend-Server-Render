package com.vpnexues.svc.service;

import com.vpnexues.svc.entity.AdminUser;
import com.vpnexues.svc.repository.AdminUserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-module access control for non-super admins (sub admins), driven by the grants the
 * super admin ticks on the Add/Edit Sub Admin form.
 *
 * <p>Grants live on {@code admin_users} as comma-separated keys:
 * <ul>
 *   <li>{@code modules} — dashboard, orders, products, users, reports, payments, support, content</li>
 *   <li>{@code permissions} — view, edit, delete, export</li>
 * </ul>
 *
 * <p>Semantics:
 * <ul>
 *   <li>SUPER_ADMIN always has access.</li>
 *   <li>A NULL/blank {@code modules} column means a legacy account created before this
 *       feature — full access, so existing sub-admins keep working until re-saved.</li>
 *   <li>Reads are also allowed through the {@code reports} module (analytics is read-only
 *       over the same data), via {@link #requireRead} / {@link #requireAnyRead}.</li>
 *   <li>Mutations require the exact module <b>and</b> the matching permission
 *       ({@link #requirePermission}); blank permissions = full permission inside the
 *       granted modules (legacy behaviour).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AdminAccessService {

    public static final String M_DASHBOARD = "dashboard";
    public static final String M_ORDERS = "orders";
    public static final String M_PRODUCTS = "products";
    public static final String M_USERS = "users";
    public static final String M_REPORTS = "reports";
    public static final String M_PAYMENTS = "payments";
    public static final String M_SUPPORT = "support";
    public static final String M_CONTENT = "content";

    public static final String P_VIEW = "view";
    public static final String P_EDIT = "edit";
    public static final String P_DELETE = "delete";
    public static final String P_EXPORT = "export";

    private final AdminUserRepository adminUserRepository;

    /** Read access to a module's data; the "reports" module implies read-only access everywhere. */
    @Transactional(readOnly = true)
    public void requireRead(UUID adminId, String module) {
        AdminUser admin = load(adminId);
        if (isUnrestricted(admin) || hasModule(admin, module) || hasModule(admin, M_REPORTS)) {
            return;
        }
        throw denied(module);
    }

    /** Read access when any of several modules qualifies (e.g. categories serve products OR content). */
    @Transactional(readOnly = true)
    public void requireAnyRead(UUID adminId, String... modules) {
        AdminUser admin = load(adminId);
        if (isUnrestricted(admin)) {
            return;
        }
        for (String module : modules) {
            if (hasModule(admin, module) || hasModule(admin, M_REPORTS)) {
                return;
            }
        }
        throw denied(modules.length > 0 ? modules[0] : "?");
    }

    /** Module access plus an action permission — used for mutations (edit/delete). */
    @Transactional(readOnly = true)
    public void requirePermission(UUID adminId, String module, String permission) {
        AdminUser admin = load(adminId);
        if (isUnrestricted(admin)) {
            return;
        }
        if (!hasModule(admin, module)) {
            throw denied(module);
        }
        if (contains(admin.getPermissions(), permission)) {
            return;
        }
        throw new AccessDeniedException(
                "You do not have '" + permission + "' permission on the '" + module + "' module.");
    }

    private AdminUser load(UUID adminId) {
        if (adminId == null) {
            throw new AccessDeniedException("Access denied");
        }
        return adminUserRepository
                .findById(adminId)
                .orElseThrow(() -> new AccessDeniedException("Access denied"));
    }

    private boolean isUnrestricted(AdminUser admin) {
        return admin.getRole() == com.vpnexues.svc.entity.AdminRole.SUPER_ADMIN
                || admin.getModules() == null
                || admin.getModules().isBlank();
    }

    private boolean hasModule(AdminUser admin, String module) {
        return contains(admin.getModules(), module);
    }

    private boolean contains(String csv, String key) {
        if (csv == null || csv.isBlank()) {
            return false;
        }
        return Arrays.stream(csv.split(",")).anyMatch(part -> part.trim().equalsIgnoreCase(key));
    }

    private AccessDeniedException denied(String module) {
        return new AccessDeniedException("You do not have access to the '" + module + "' module.");
    }

    /** Parses a stored comma-separated grant column into a list (empty for legacy/blank). */
    public static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
