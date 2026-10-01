package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.ChatSessionStatus;
import com.vpnexues.svc.entity.SupportChatSession;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportChatSessionRepository extends JpaRepository<SupportChatSession, UUID> {

    List<SupportChatSession> findAllByOrderByCreatedAtDesc();

    List<SupportChatSession> findByStatusOrderByCreatedAtDesc(ChatSessionStatus status);
}
