package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.Testimonial;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestimonialRepository extends JpaRepository<Testimonial, UUID> {

    List<Testimonial> findAllByOrderByCreatedAtDesc();

    List<Testimonial> findByActiveTrueOrderByCreatedAtDesc();
}
