package com.vpnexues.svc.service;

import com.vpnexues.svc.entity.AdminLoginAudit;
import com.vpnexues.svc.entity.AdminUser;
import com.vpnexues.svc.repository.AdminLoginAuditRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records admin login attempts for security audit trail.
 * Never logs passwords, tokens, or sensitive credential data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AdminLoginAuditRepository auditRepository;

    @Transactional
    public void recordLoginAttempt(
            AdminUser admin,
            String emailAttempted,
            boolean success,
            String failureReason,
            HttpServletRequest request) {
        AdminLoginAudit audit = new AdminLoginAudit();
        audit.setAdmin(admin);
        audit.setEmailAttempted(emailAttempted);
        audit.setSuccess(success);
        audit.setFailureReason(failureReason);
        audit.setIpAddress(extractClientIp(request));
        audit.setUserAgent(request != null ? request.getHeader("User-Agent") : null);
        auditRepository.save(audit);

        if (success) {
            log.info("Admin login SUCCESS: email={}, ip={}", emailAttempted, audit.getIpAddress());
        } else {
            log.warn("Admin login FAILED: email={}, reason={}, ip={}", emailAttempted, failureReason, audit.getIpAddress());
        }
    }

    @Transactional
    public void recordSecurityEvent(String email, String reason, HttpServletRequest request) {
        AdminLoginAudit audit = new AdminLoginAudit();
        audit.setEmailAttempted(email);
        audit.setSuccess(false);
        audit.setFailureReason(reason);
        audit.setIpAddress(extractClientIp(request));
        audit.setUserAgent(request != null ? request.getHeader("User-Agent") : null);
        auditRepository.save(audit);

        log.warn("Admin security event: email={}, reason={}, ip={}", email, reason, audit.getIpAddress());
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return null;
        // Check common proxy headers first
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For may contain multiple IPs; take the first (client)
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
