package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.AdminLoginAudit;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminLoginAuditRepository extends JpaRepository<AdminLoginAudit, UUID> {
}
