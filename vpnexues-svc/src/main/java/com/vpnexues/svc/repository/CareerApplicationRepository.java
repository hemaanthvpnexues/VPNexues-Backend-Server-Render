package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.CareerApplication;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareerApplicationRepository extends JpaRepository<CareerApplication, UUID> {

    List<CareerApplication> findAllByOrderByCreatedAtDesc();
}
