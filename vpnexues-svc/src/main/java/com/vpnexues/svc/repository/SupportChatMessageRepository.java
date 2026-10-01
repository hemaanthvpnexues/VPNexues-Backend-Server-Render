package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.SupportChatMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportChatMessageRepository extends JpaRepository<SupportChatMessage, UUID> {

    List<SupportChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    void deleteBySessionId(UUID sessionId);
}
