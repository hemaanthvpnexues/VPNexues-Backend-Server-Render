package com.vpnexues.svc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "support_chat_sessions")
public class SupportChatSession extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String visitorName;

    @Column(length = 255)
    private String visitorEmail;

    @Column(name = "country_code", length = 4)
    private String countryCode;

    @Column(nullable = false, length = 200)
    private String subject = "Live chat escalation";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatSessionStatus status = ChatSessionStatus.OPEN;

    private Instant closedAt;
}
