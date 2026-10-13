package com.vpnexues.svc.repository;

import com.vpnexues.svc.entity.SupportChatMessage;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupportChatMessageRepository extends JpaRepository<SupportChatMessage, UUID> {

    List<SupportChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    /**
     * One query for every session on the list, instead of one per session —
     * the Support page previously issued N+1 queries on load. Ordered by
     * session then time so callers can group consecutively.
     */
    @Query("""
            SELECT m FROM SupportChatMessage m
            WHERE m.session.id IN :sessionIds
            ORDER BY m.session.id ASC, m.createdAt ASC
            """)
    List<SupportChatMessage> findBySessionIdInOrderByCreatedAtAsc(
            @Param("sessionIds") Collection<UUID> sessionIds);

    void deleteBySessionId(UUID sessionId);
}
