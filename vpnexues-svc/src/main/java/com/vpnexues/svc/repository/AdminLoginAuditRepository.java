package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.AdminLoginAudit;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminLoginAuditRepository extends JpaRepository<AdminLoginAudit, UUID> {

    @Query("SELECT MAX(a.attemptedAt) FROM AdminLoginAudit a WHERE a.admin.id = :adminId AND a.success = true")
    Optional<Instant> findLatestSuccessfulLoginByAdminId(@Param("adminId") UUID adminId);

    /**
     * Batch version used by the Sub Admins list. The previous code called the
     * single-admin query once per row, so loading N admins issued N round trips.
     * One grouped query replaces them all.
     */
    @Query("""
            SELECT a.admin.id AS adminId, MAX(a.attemptedAt) AS latestAt
            FROM AdminLoginAudit a
            WHERE a.success = true AND a.admin.id IN :adminIds
            GROUP BY a.admin.id
            """)
    List<LatestLoginProjection> findLatestSuccessfulLogins(
            @Param("adminIds") Collection<UUID> adminIds);

    /** Interface projection — Spring Data maps by getter-matching JPQL alias. */
    interface LatestLoginProjection {
        UUID getAdminId();

        Instant getLatestAt();
    }
}
