package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.AdminAccessPass;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAccessPassRepository extends JpaRepository<AdminAccessPass, UUID> {

    /** The pass the login gate consults — newest row wins (generating kills the older ones). */
    Optional<AdminAccessPass> findTopByAdminIdOrderByCreatedAtDesc(UUID adminId);

    /** Newest-first audit trail for the Sub-Admin detail screen. */
    List<AdminAccessPass> findTop20ByAdminIdOrderByCreatedAtDesc(UUID adminId);

    /** Still in the wild — invalidated on re-issue so only one pass per account is live. */
    List<AdminAccessPass> findByAdminIdAndUsedAtIsNullAndInvalidatedFalse(UUID adminId);
}
