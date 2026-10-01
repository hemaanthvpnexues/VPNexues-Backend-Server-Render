package com.vpnexues.svc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "support_chat_messages")
public class SupportChatMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private SupportChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChatMessageSender sender = ChatMessageSender.VISITOR;

    @Column(nullable = false, columnDefinition = "text")
    private String body;
}
