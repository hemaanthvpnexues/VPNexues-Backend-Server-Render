package com.vpnexues.svc.service;

import com.vpnexues.svc.entity.AdminUser;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.AdminUserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authenticate admin by email and password.
     * The controller is responsible for brute-force checking BEFORE calling this method.
     * This method only validates credentials and active status.
     */
    public AdminUser authenticate(String email, String password) {
        AdminUser admin = adminUserRepository
                .findByEmail(email)
                .filter(AdminUser::isActive)
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }
        return admin;
    }

    public void changePassword(UUID adminId, String currentPassword, String newPassword) {
        AdminUser admin =
                adminUserRepository.findById(adminId).orElseThrow(() -> new NotFoundException("Admin not found"));

        if (!passwordEncoder.matches(currentPassword, admin.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        admin.setPasswordHash(passwordEncoder.encode(newPassword));
        // Increment token version to invalidate all existing access tokens
        admin.setTokenVersion(admin.getTokenVersion() + 1);
        // Clear mustChangePassword flag if it was set
        admin.setMustChangePassword(false);
        adminUserRepository.save(admin);
    }
}
