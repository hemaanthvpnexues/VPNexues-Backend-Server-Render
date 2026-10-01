package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.ContactMessage;
import com.vpnexues.svc.entity.ContactMessageStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, UUID> {

    List<ContactMessage> findAllByOrderByCreatedAtDesc();

    List<ContactMessage> findByStatusOrderByCreatedAtDesc(ContactMessageStatus status);

    List<ContactMessage> findByChatSessionId(UUID sessionId);
}
