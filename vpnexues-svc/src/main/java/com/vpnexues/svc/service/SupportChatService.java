package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.ChatMessageDto;
import com.vpnexues.svc.dto.ChatSessionDto;
import com.vpnexues.svc.dto.CreateChatSessionRequest;
import com.vpnexues.svc.dto.PostChatMessageRequest;
import com.vpnexues.svc.dto.UpdateChatContactRequest;
import com.vpnexues.svc.entity.ChatMessageSender;
import com.vpnexues.svc.entity.ChatSessionStatus;
import com.vpnexues.svc.entity.ContactMessage;
import com.vpnexues.svc.entity.SupportChatMessage;
import com.vpnexues.svc.entity.SupportChatSession;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.ContactMessageRepository;
import com.vpnexues.svc.repository.SupportChatMessageRepository;
import com.vpnexues.svc.repository.SupportChatSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Live-chat transcript store backing the storefront chatbot's "Talk to Agent"
 * escalation. Guests file a session (UUID, unguessable) and every message —
 * visitor, canned bot, admin reply — is persisted for the sub/super admin
 * support inboxes. Email notification is fail-open (see
 * {@link ContactFormMailService}): a mail outage never fails the request.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SupportChatService {

    private static final Logger log = LoggerFactory.getLogger(SupportChatService.class);

    private final SupportChatSessionRepository sessionRepository;
    private final SupportChatMessageRepository messageRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final ContactFormMailService mailService;

    public ChatSessionDto createSession(CreateChatSessionRequest req) {
        SupportChatSession session = new SupportChatSession();
        session.setVisitorName(req.visitorName().trim());
        session.setVisitorEmail(req.visitorEmail() == null ? null : req.visitorEmail().trim());
        session.setCountryCode(req.countryCode() == null ? null : req.countryCode().trim());
        session.setSubject(req.subject() == null || req.subject().isBlank()
                ? "Live chat escalation"
                : req.subject().trim());
        session.setStatus(ChatSessionStatus.OPEN);
        SupportChatSession saved = sessionRepository.save(session);
        return toDto(saved, List.of());
    }

    /**
     * Attaches the visitor's contact to a provisional guest session. The first
     * call that provides an email also mirrors the first visitor message into
     * the contact-messages inbox and sends the transcript email (fail-open).
     */
    public ChatSessionDto updateContact(UUID sessionId, UpdateChatContactRequest req) {
        SupportChatSession session = getSession(sessionId);
        if (session.getStatus() == ChatSessionStatus.CLOSED) {
            throw new BadRequestException("Chat session is closed: " + sessionId);
        }
        boolean newlyIdentified = session.getVisitorEmail() == null || session.getVisitorEmail().isBlank();
        session.setVisitorName(req.visitorName().trim());
        session.setVisitorEmail(req.visitorEmail().trim());
        SupportChatSession saved = sessionRepository.save(session);
        List<SupportChatMessage> transcript = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        if (newlyIdentified) {
            backfillMirrorIdentity(saved);
            boolean hasMirror = !contactMessageRepository.findByChatSessionId(saved.getId()).isEmpty();
            if (!hasMirror) {
                String firstVisitor = transcript.stream()
                        .filter(m -> m.getSender() == ChatMessageSender.VISITOR)
                        .map(SupportChatMessage::getBody)
                        .findFirst()
                        .orElse(saved.getSubject());
                mirrorToContactInbox(saved, firstVisitor);
            }
            notifyAdmins(saved, transcript);
        }
        return toDto(saved, transcript.stream().map(this::toMessageDto).toList());
    }

    public ChatMessageDto postMessage(UUID sessionId, PostChatMessageRequest req) {
        SupportChatSession session = getSession(sessionId);
        if (session.getStatus() == ChatSessionStatus.CLOSED) {
            throw new BadRequestException("Chat session is closed: " + sessionId);
        }
        ChatMessageSender sender;
        try {
            sender = ChatMessageSender.valueOf(req.sender());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid sender: " + req.sender());
        }
        SupportChatMessage message = new SupportChatMessage();
        message.setSession(session);
        message.setSender(sender);
        message.setBody(req.body().trim());
        boolean firstVisitorMessage = sender == ChatMessageSender.VISITOR
                && messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                        .noneMatch(m -> m.getSender() == ChatMessageSender.VISITOR);
        SupportChatMessage saved = messageRepository.save(message);
        if (firstVisitorMessage) {
            mirrorToContactInbox(session, saved.getBody());
        }
        if (sender == ChatMessageSender.ADMIN && session.getStatus() == ChatSessionStatus.OPEN) {
            session.setStatus(ChatSessionStatus.IN_PROGRESS);
            sessionRepository.save(session);
        }
        return toMessageDto(saved);
    }

    /** Escalation email with the transcript so far — best-effort, never throws. */
    public void notifyAdmins(SupportChatSession session, List<SupportChatMessage> transcript) {
        List<String> lines = transcript.stream()
                .map(m -> m.getSender().name().toLowerCase() + ": " + stripHtml(m.getBody()))
                .toList();
        mailService.sendChatTranscriptEmail(
                session.getVisitorName(), session.getVisitorEmail(), session.getSubject(), lines);
    }

    /**
     * Mirrors the visitor's first chat message as a row in the familiar
     * contact-messages inbox (SUP ticket table) so sub/super admins see it
     * exactly like a contact-form submission, scoped by country — even for
     * guests who never identify. The row subject stays a fixed "Live chat";
     * the visitor's message lives in the message field, shown only in the
     * detail drawer (view button). The row links back via chatSessionId and
     * is backfilled with name/email if contact arrives later. No email here —
     * the escalation transcript email already covers notification.
     * Best-effort: failures are logged, never thrown.
     */
    private void mirrorToContactInbox(SupportChatSession session, String body) {
        try {
            ContactMessage row = new ContactMessage();
            row.setName(session.getVisitorName());
            row.setEmail(session.getVisitorEmail());
            row.setCountryCode(session.getCountryCode());
            row.setSubject("Live chat");
            row.setMessage(body);
            row.setChatSessionId(session.getId());
            contactMessageRepository.save(row);
        } catch (Exception ex) {
            log.warn("Failed to mirror chat {} to contact inbox: {}", session.getId(), ex.getMessage());
        }
    }

    /** Backfills the mirrored ticket row once the guest identifies. */
    private void backfillMirrorIdentity(SupportChatSession session) {
        try {
            List<ContactMessage> rows = contactMessageRepository.findByChatSessionId(session.getId());
            for (ContactMessage row : rows) {
                row.setName(session.getVisitorName());
                row.setEmail(session.getVisitorEmail());
                contactMessageRepository.save(row);
            }
        } catch (Exception ex) {
            log.warn("Failed to backfill chat mirror {}: {}", session.getId(), ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<ChatSessionDto> list(ChatSessionStatus status) {
        List<SupportChatSession> sessions = status == null
                ? sessionRepository.findAllByOrderByCreatedAtDesc()
                : sessionRepository.findByStatusOrderByCreatedAtDesc(status);
        return sessions.stream()
                .map(s -> toDto(s, messageRepository.findBySessionIdOrderByCreatedAtAsc(s.getId()).stream()
                        .map(this::toMessageDto)
                        .toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatSessionDto getTranscript(UUID sessionId) {
        SupportChatSession session = getSession(sessionId);
        List<ChatMessageDto> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(this::toMessageDto)
                .toList();
        return toDto(session, messages);
    }

    public ChatSessionDto updateStatus(UUID sessionId, ChatSessionStatus status) {
        SupportChatSession session = getSession(sessionId);
        session.setStatus(status);
        if (status == ChatSessionStatus.CLOSED) {
            session.setClosedAt(Instant.now());
        }
        SupportChatSession saved = sessionRepository.save(session);
        return toDto(saved, messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(this::toMessageDto)
                .toList());
    }

    public void delete(UUID sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new NotFoundException("Chat session not found: " + sessionId);
        }
        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
    }

    private SupportChatSession getSession(UUID sessionId) {
        return sessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Chat session not found: " + sessionId));
    }

    private ChatSessionDto toDto(SupportChatSession s, List<ChatMessageDto> messages) {
        return new ChatSessionDto(
                s.getId(),
                s.getVisitorName(),
                s.getVisitorEmail(),
                s.getCountryCode(),
                s.getSubject(),
                s.getStatus().name(),
                s.getCreatedAt(),
                s.getClosedAt(),
                messages);
    }

    private ChatMessageDto toMessageDto(SupportChatMessage m) {
        return new ChatMessageDto(m.getId(), m.getSender().name(), m.getBody(), m.getCreatedAt());
    }

    /** Bot messages carry markup — plain-text email gets the readable text only. */
    static String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }
}
