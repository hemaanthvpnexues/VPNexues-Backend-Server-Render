package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.User;
import com.vpnexues.svc.entity.UserDevice;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {

    Optional<UserDevice> findByUserAndDeviceTokenHashAndRevokedAtIsNull(User user, String deviceTokenHash);

    /** Active device with the oldest last-seen time — the eviction candidate when the cap is hit. */
    Optional<UserDevice> findFirstByUserAndRevokedAtIsNullOrderByLastSeenAtAsc(User user);

    long countByUserAndRevokedAtIsNull(User user);
}
