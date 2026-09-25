package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByPhone(String phone);

    Optional<User> findByFirebaseUid(String firebaseUid);
}
