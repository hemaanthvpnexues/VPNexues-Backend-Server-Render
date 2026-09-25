package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.B2BEnquiry;
import com.vpnexues.svc.entity.B2BEnquiryStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface B2BEnquiryRepository extends JpaRepository<B2BEnquiry, UUID> {

    List<B2BEnquiry> findAllByOrderByCreatedAtDesc();

    List<B2BEnquiry> findByStatusOrderByCreatedAtDesc(B2BEnquiryStatus status);
}
