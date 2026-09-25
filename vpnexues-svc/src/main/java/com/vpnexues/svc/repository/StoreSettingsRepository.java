package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.StoreSettings;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreSettingsRepository extends JpaRepository<StoreSettings, UUID> {

    /** Single-row table (see R__Seed_data.sql) — this is always "the" settings row. */
    Optional<StoreSettings> findFirstByOrderByCreatedAtAsc();
}
