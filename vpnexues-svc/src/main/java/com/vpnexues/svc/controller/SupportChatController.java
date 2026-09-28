package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.ChatMessageDto;
import com.vpnexues.svc.dto.ChatSessionDto;
import com.vpnexues.svc.dto.CreateChatSessionRequest;
import com.vpnexues.svc.dto.PostChatMessageRequest;
import com.vpnexues.svc.dto.UpdateChatContactRequest;
import com.vpnexues.svc.entity.ChatMessageSender;
import com.vpnexues.svc.entity.ChatSessionStatus;
import com.vpnexues.svc.entity.SupportChatMessage;
import com.vpnexues.svc.entity.SupportChatSession;
import com.vpnexues.svc.repository.SupportChatMessageRepository;
import com.vpnexues.svc.repository.SupportChatSessionRepository;
import com.vpnexues.svc.service.SupportChatService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Live-chat transcript endpoints. Guest endpoints (create, message, read-own)
 * are public — the session UUID is unguessable — and mirror the public
 * contact-message flow. Admin endpoints live under /api/admin/** and reuse
 * the existing admin auth chain.
 */
@RestController
@RequiredArgsConstructor
public class SupportChatController {

    private final SupportChatService supportChatService;
    private final SupportChatSessionRepository sessionRepository;
    private final SupportChatMessageRepository messageRepository;

    // ── Guest ──────────────────────────────────────────────────────────────

    @PostMapping("/api/support-chats")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatSessionDto createSession(@RequestBody @Valid CreateChatSessionRequest req) {
        return supportChatService.createSession(req);
    }

    @PutMapping("/api/support-chats/{id}/contact")
    public ChatSessionDto updateContact(
            @PathVariable UUID id, @RequestBody @Valid UpdateChatContactRequest req) {
        return supportChatService.updateContact(id, req);
    }

    @PostMapping("/api/support-chats/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageDto postMessage(
            @PathVariable UUID id, @RequestBody @Valid PostChatMessageRequest req) {
        if (ChatMessageSender.valueOf(req.sender()) == ChatMessageSender.ADMIN) {
            throw new com.vpnexues.svc.exception.BadRequestException("Only admins can post as ADMIN");
        }
        return supportChatService.postMessage(id, req);
    }

    @GetMapping("/api/support-chats/{id}")
    public ChatSessionDto getSession(@PathVariable UUID id) {
        return supportChatService.getTranscript(id);
    }

    // ── Admin ──────────────────────────────────────────────────────────────

    @GetMapping("/api/admin/support-chats")
    public List<ChatSessionDto> list(@RequestParam(required = false) ChatSessionStatus status) {
        return supportChatService.list(status);
    }

    @PostMapping("/api/admin/support-chats/{id}/reply")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageDto reply(
            @PathVariable UUID id, @RequestBody @Valid PostChatMessageRequest req) {
        return supportChatService.postMessage(
                id, new PostChatMessageRequest(ChatMessageSender.ADMIN.name(), req.body()));
    }

    @PutMapping("/api/admin/support-chats/{id}/status")
    public ChatSessionDto updateStatus(
            @PathVariable UUID id, @RequestParam ChatSessionStatus status) {
        return supportChatService.updateStatus(id, status);
    }

    @DeleteMapping("/api/admin/support-chats/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        supportChatService.delete(id);
    }

    /** Full transcript email on demand (e.g. visitor clicks "email me this chat"). */
    @PostMapping("/api/support-chats/{id}/notify")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void notify(@PathVariable UUID id) {
        SupportChatSession session = sessionRepository.findById(id).orElseThrow(
                () -> new com.vpnexues.svc.exception.NotFoundException("Chat session not found: " + id));
        List<SupportChatMessage> transcript = messageRepository.findBySessionIdOrderByCreatedAtAsc(id);
        supportChatService.notifyAdmins(session, transcript);
    }
}
