package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.CareerJob;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareerJobRepository extends JpaRepository<CareerJob, UUID> {

    List<CareerJob> findAllByOrderByDisplayOrderAscCreatedAtAsc();

    List<CareerJob> findByActiveTrueOrderByDisplayOrderAscCreatedAtAsc();

    boolean existsBySlug(String slug);
}
