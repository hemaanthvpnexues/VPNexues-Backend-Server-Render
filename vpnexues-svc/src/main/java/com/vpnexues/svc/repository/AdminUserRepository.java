package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.AdminUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {

    /**
     * Case-insensitive on purpose. Login lower-cases the submitted address before
     * calling this (AdminAuthController) and V52 lower-cases stored rows — but
     * without {@code lower()} here, a single row left in mixed case would be
     * unreachable forever. That mismatch is exactly the login failure this query
     * now guards against.
     *
     * <p>admin_users holds a handful of rows, so giving up the plain index lookup
     * costs nothing.
     */
    @Query("SELECT a FROM AdminUser a WHERE lower(a.email) = lower(:email)")
    Optional<AdminUser> findByEmail(@Param("email") String email);
}
