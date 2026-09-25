package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.AdminRefreshToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface AdminRefreshTokenRepository extends JpaRepository<AdminRefreshToken, UUID> {

    Optional<AdminRefreshToken> findByTokenHash(String tokenHash);

    List<AdminRefreshToken> findByAdminIdAndRevokedFalse(UUID adminId);

    @Modifying
    @Transactional
    @Query("UPDATE AdminRefreshToken t SET t.revoked = true, t.revokedAt = CURRENT_TIMESTAMP "
            + "WHERE t.admin.id = :adminId AND t.revoked = false")
    int revokeAllByAdminId(UUID adminId);

    @Modifying
    @Transactional
    @Query("DELETE FROM AdminRefreshToken t WHERE t.expiresAt < :now")
    int deleteExpired(java.time.Instant now);
}
