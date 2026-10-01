package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.AdminLoginAudit;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminLoginAuditRepository extends JpaRepository<AdminLoginAudit, UUID> {

    @Query("SELECT MAX(a.attemptedAt) FROM AdminLoginAudit a WHERE a.admin.id = :adminId AND a.success = true")
    Optional<Instant> findLatestSuccessfulLoginByAdminId(@Param("adminId") UUID adminId);
}
