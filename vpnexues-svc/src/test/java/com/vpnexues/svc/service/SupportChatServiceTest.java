package com.vpnexues.svc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vpnexues.svc.dto.ChatMessageDto;
import com.vpnexues.svc.dto.ChatSessionDto;
import com.vpnexues.svc.dto.CreateChatSessionRequest;
import com.vpnexues.svc.dto.PostChatMessageRequest;
import com.vpnexues.svc.entity.ChatMessageSender;
import com.vpnexues.svc.entity.ChatSessionStatus;
import com.vpnexues.svc.entity.SupportChatMessage;
import com.vpnexues.svc.entity.SupportChatSession;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.repository.SupportChatMessageRepository;
import com.vpnexues.svc.repository.SupportChatSessionRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

/** Unit tests for the live-chat transcript flow — no DB, no Spring context. */
class SupportChatServiceTest {

    private InMemorySessions sessions;
    private InMemoryMessages messages;
    private InMemoryContactMessages inbox;
    private SupportChatService service;

    @BeforeEach
    void setUp() {
        sessions = new InMemorySessions();
        messages = new InMemoryMessages();
        inbox = new InMemoryContactMessages();
        service = new SupportChatService(sessions, messages, inbox, new NoopMailService());
    }

    @Test
    void createSessionTrimsAndDefaultsSubject() {
        ChatSessionDto dto = service.createSession(
                new CreateChatSessionRequest("  Priya  ", "priya@email.com", "IN", "  "));
        assertEquals("Priya", dto.visitorName());
        assertEquals("Live chat escalation", dto.subject());
        assertEquals("OPEN", dto.status());
    }

    @Test
    void visitorMessagePersistsAndAdminReplyMovesToInProgress() {
        ChatSessionDto dto = service.createSession(
                new CreateChatSessionRequest("A", "a@b.com", null, "Help"));
        ChatMessageDto m = service.postMessage(dto.id(), new PostChatMessageRequest("VISITOR", "  Hello  "));
        assertEquals("Hello", m.body());
        service.postMessage(dto.id(), new PostChatMessageRequest("ADMIN", "Hi, how can I help?"));
        assertEquals("IN_PROGRESS", sessions.store.get(dto.id()).getStatus().name());
    }

    @Test
    void closedSessionRejectsNewMessages() {
        ChatSessionDto dto = service.createSession(
                new CreateChatSessionRequest("A", "a@b.com", null, "Help"));
        service.updateStatus(dto.id(), ChatSessionStatus.CLOSED);
        assertThrows(BadRequestException.class, () ->
                service.postMessage(dto.id(), new PostChatMessageRequest("VISITOR", "Hello?")));
    }

    @Test
    void invalidSenderRejected() {
        ChatSessionDto dto = service.createSession(
                new CreateChatSessionRequest("A", "a@b.com", null, "Help"));
        assertThrows(BadRequestException.class, () ->
                service.postMessage(dto.id(), new PostChatMessageRequest("ROBOT", "beep")));
    }

    @Test
    void stripHtmlKeepsReadableText() {
        assertEquals("Hello world", SupportChatService.stripHtml("<b>Hello</b> <i>world</i>"));
    }

    @Test
    void firstVisitorMessageMirroredToContactInboxOnce() {        ChatSessionDto dto = service.createSession(
                new CreateChatSessionRequest("A", "a@b.com", "IN", "Live chat: refund?"));
        service.postMessage(dto.id(), new PostChatMessageRequest("BOT", "Hi there"));
        assertEquals(0, inbox.store.size());
        service.postMessage(dto.id(), new PostChatMessageRequest("VISITOR", "I need a refund"));
        assertEquals(1, inbox.store.size());
        assertEquals("I need a refund", inbox.store.get(0).getMessage());
        assertEquals("IN", inbox.store.get(0).getCountryCode());
        service.postMessage(dto.id(), new PostChatMessageRequest("VISITOR", "Hello?"));
        assertEquals(1, inbox.store.size());
    }

    @Test
    void provisionalGuestSessionStoredWithoutEmail() {
        ChatSessionDto dto = service.createSession(
                new CreateChatSessionRequest("Chat visitor", null, "SG", "Live chat: pricing?"));
        service.postMessage(dto.id(), new PostChatMessageRequest("VISITOR", "pricing?"));
        // Guest ticket row files instantly (no email yet) + transcript stored.
        assertEquals(1, inbox.store.size());
        assertEquals("Chat visitor", inbox.store.get(0).getName());
        assertEquals(1, messages.findBySessionIdOrderByCreatedAtAsc(dto.id()).size());
    }

    @Test
    void updateContactMirrorsOnceAndIdentifies() {
        ChatSessionDto dto = service.createSession(
                new CreateChatSessionRequest("Chat visitor", null, null, "Live chat: hi"));
        service.postMessage(dto.id(), new PostChatMessageRequest("VISITOR", "hi there"));
        ChatSessionDto updated = service.updateContact(dto.id(),
                new com.vpnexues.svc.dto.UpdateChatContactRequest("Priya", "priya@email.com"));
        assertEquals("Priya", updated.visitorName());
        assertEquals("priya@email.com", updated.visitorEmail());
        assertEquals(1, inbox.store.size());
        assertEquals("hi there", inbox.store.get(0).getMessage());
        service.updateContact(dto.id(),
                new com.vpnexues.svc.dto.UpdateChatContactRequest("Priya S", "priya@email.com"));
        assertEquals(1, inbox.store.size());
    }

    @Test
    void updateContactOnClosedSessionRejected() {
        ChatSessionDto dto = service.createSession(
                new CreateChatSessionRequest("Chat visitor", null, null, "Live chat: hi"));
        service.updateStatus(dto.id(), ChatSessionStatus.CLOSED);
        assertThrows(BadRequestException.class, () -> service.updateContact(dto.id(),
                new com.vpnexues.svc.dto.UpdateChatContactRequest("A", "a@b.com")));
    }

    @Test
    void guestFirstMessageMirrorsInstantlyAndBackfillsOnContact() {
        ChatSessionDto dto = service.createSession(
                new CreateChatSessionRequest("Chat visitor", null, "IN", "Live chat: hello"));
        service.postMessage(dto.id(), new PostChatMessageRequest("VISITOR", "hello vpnexues"));
        assertEquals(1, inbox.store.size());
        assertEquals("Chat visitor", inbox.store.get(0).getName());
        assertEquals("Live chat", inbox.store.get(0).getSubject());
        assertEquals("hello vpnexues", inbox.store.get(0).getMessage());
        assertEquals(dto.id(), inbox.store.get(0).getChatSessionId());
        service.updateContact(dto.id(),
                new com.vpnexues.svc.dto.UpdateChatContactRequest("Jagatheesh", "jaga@email.com"));
        assertEquals(1, inbox.store.size());
        assertEquals("Jagatheesh", inbox.store.get(0).getName());
        assertEquals("jaga@email.com", inbox.store.get(0).getEmail());
    }

    // ── Fakes ──────────────────────────────────────────────────────────────

    static class InMemorySessions implements SupportChatSessionRepository {
        final Map<UUID, SupportChatSession> store = new HashMap<>();

        @Override
        public <S extends SupportChatSession> S save(S entity) {
            if (entity.getId() == null) {
                try {
                    var idField = entity.getClass().getSuperclass().getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(entity, UUID.randomUUID());
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            store.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Optional<SupportChatSession> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public boolean existsById(UUID id) {
            return store.containsKey(id);
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }

        @Override
        public List<SupportChatSession> findAllByOrderByCreatedAtDesc() {
            return new ArrayList<>(store.values());
        }

        @Override
        public List<SupportChatSession> findByStatusOrderByCreatedAtDesc(ChatSessionStatus status) {
            return store.values().stream().filter(s -> s.getStatus() == status).toList();
        }

        @Override
        public List<SupportChatSession> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public List<SupportChatSession> findAll(Sort sort) {
            return findAll();
        }

        @Override
        public Page<SupportChatSession> findAll(Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SupportChatSession> findAllById(Iterable<UUID> ids) {
            List<SupportChatSession> out = new ArrayList<>();
            ids.forEach(id -> {
                SupportChatSession s = store.get(id);
                if (s != null) {
                    out.add(s);
                }
            });
            return out;
        }

        @Override
        public <S extends SupportChatSession> List<S> saveAll(Iterable<S> entities) {
            List<S> out = new ArrayList<>();
            entities.forEach(e -> out.add(save(e)));
            return out;
        }

        @Override
        public <S extends SupportChatSession> Optional<S> findOne(Example<S> example) {
            return Optional.empty();
        }

        @Override
        public <S extends SupportChatSession> List<S> findAll(Example<S> example) {
            return List.of();
        }

        @Override
        public <S extends SupportChatSession> List<S> findAll(Example<S> example, Sort sort) {
            return List.of();
        }

        @Override
        public <S extends SupportChatSession> Page<S> findAll(Example<S> example, Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends SupportChatSession> long count(Example<S> example) {
            return 0;
        }

        @Override
        public <S extends SupportChatSession> boolean exists(Example<S> example) {
            return false;
        }

        @Override
        public <S extends SupportChatSession, R> R findBy(
                Example<S> example, java.util.function.Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public void delete(SupportChatSession entity) {
            store.remove(entity.getId());
        }

        @Override
        public void deleteAllById(Iterable<? extends UUID> ids) {
            ids.forEach(store::remove);
        }

        @Override
        public void deleteAll(Iterable<? extends SupportChatSession> entities) {
            entities.forEach(e -> store.remove(e.getId()));
        }

        @Override
        public void deleteAll() {
            store.clear();
        }

        @Override
        public void flush() {
        }

        @Override
        public <S extends SupportChatSession> S saveAndFlush(S entity) {
            return save(entity);
        }

        @Override
        public <S extends SupportChatSession> List<S> saveAllAndFlush(Iterable<S> entities) {
            return saveAll(entities);
        }

        @Override
        public void deleteAllInBatch(Iterable<SupportChatSession> entities) {
            deleteAll(entities);
        }

        @Override
        public void deleteAllByIdInBatch(Iterable<UUID> ids) {
            deleteAllById(ids);
        }

        @Override
        public void deleteAllInBatch() {
            store.clear();
        }

        @Override
        public SupportChatSession getOne(UUID id) {
            return store.get(id);
        }

        @Override
        public SupportChatSession getById(UUID id) {
            return store.get(id);
        }

        @Override
        public SupportChatSession getReferenceById(UUID id) {
            return store.get(id);
        }
    }

    static class InMemoryMessages implements SupportChatMessageRepository {
        final List<SupportChatMessage> store = new ArrayList<>();

        @Override
        public <S extends SupportChatMessage> S save(S entity) {
            if (entity.getId() == null) {
                try {
                    var idField = entity.getClass().getSuperclass().getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(entity, UUID.randomUUID());
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            store.add(entity);
            return entity;
        }

        @Override
        public List<SupportChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId) {
            return store.stream().filter(m -> m.getSession().getId().equals(sessionId)).toList();
        }

        @Override
        public void deleteBySessionId(UUID sessionId) {
            store.removeIf(m -> m.getSession().getId().equals(sessionId));
        }

        @Override
        public Optional<SupportChatMessage> findById(UUID id) {
            return store.stream().filter(m -> id.equals(m.getId())).findFirst();
        }

        @Override
        public boolean existsById(UUID id) {
            return store.stream().anyMatch(m -> id.equals(m.getId()));
        }

        @Override
        public List<SupportChatMessage> findAll() {
            return new ArrayList<>(store);
        }

        @Override
        public List<SupportChatMessage> findAll(Sort sort) {
            return findAll();
        }

        @Override
        public Page<SupportChatMessage> findAll(Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SupportChatMessage> findAllById(Iterable<UUID> ids) {
            List<SupportChatMessage> out = new ArrayList<>();
            ids.forEach(id -> findById(id).ifPresent(out::add));
            return out;
        }

        @Override
        public <S extends SupportChatMessage> List<S> saveAll(Iterable<S> entities) {
            List<S> out = new ArrayList<>();
            entities.forEach(e -> out.add(save(e)));
            return out;
        }

        @Override
        public <S extends SupportChatMessage> Optional<S> findOne(Example<S> example) {
            return Optional.empty();
        }

        @Override
        public <S extends SupportChatMessage> List<S> findAll(Example<S> example) {
            return List.of();
        }

        @Override
        public <S extends SupportChatMessage> List<S> findAll(Example<S> example, Sort sort) {
            return List.of();
        }

        @Override
        public <S extends SupportChatMessage> Page<S> findAll(Example<S> example, Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends SupportChatMessage> long count(Example<S> example) {
            return 0;
        }

        @Override
        public <S extends SupportChatMessage> boolean exists(Example<S> example) {
            return false;
        }

        @Override
        public <S extends SupportChatMessage, R> R findBy(
                Example<S> example, java.util.function.Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public void deleteById(UUID id) {
            store.removeIf(m -> id.equals(m.getId()));
        }

        @Override
        public void delete(SupportChatMessage entity) {
            store.remove(entity);
        }

        @Override
        public void deleteAllById(Iterable<? extends UUID> ids) {
            ids.forEach(this::deleteById);
        }

        @Override
        public void deleteAll(Iterable<? extends SupportChatMessage> entities) {
            entities.forEach(store::remove);
        }

        @Override
        public void deleteAll() {
            store.clear();
        }

        @Override
        public void flush() {
        }

        @Override
        public <S extends SupportChatMessage> S saveAndFlush(S entity) {
            return save(entity);
        }

        @Override
        public <S extends SupportChatMessage> List<S> saveAllAndFlush(Iterable<S> entities) {
            return saveAll(entities);
        }

        @Override
        public void deleteAllInBatch(Iterable<SupportChatMessage> entities) {
            deleteAll(entities);
        }

        @Override
        public void deleteAllByIdInBatch(Iterable<UUID> ids) {
            deleteAllById(ids);
        }

        @Override
        public void deleteAllInBatch() {
            store.clear();
        }

        @Override
        public SupportChatMessage getOne(UUID id) {
            return findById(id).orElse(null);
        }

        @Override
        public SupportChatMessage getById(UUID id) {
            return findById(id).orElse(null);
        }

        @Override
        public SupportChatMessage getReferenceById(UUID id) {
            return findById(id).orElse(null);
        }
    }

    /** ContactMessageRepository fake — only save/query used by the mirror path. */
    static class InMemoryContactMessages implements com.vpnexues.svc.repository.ContactMessageRepository {
        final List<com.vpnexues.svc.entity.ContactMessage> store = new ArrayList<>();

        @Override
        @SuppressWarnings("unchecked")
        public <S extends com.vpnexues.svc.entity.ContactMessage> S save(S entity) {
            if (entity.getId() == null) {
                try {
                    var idField = entity.getClass().getSuperclass().getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(entity, UUID.randomUUID());
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            } else {
                UUID id = entity.getId();
                store.removeIf(m -> id.equals(m.getId()));
            }
            store.add(entity);
            return entity;
        }

        @Override
        public List<com.vpnexues.svc.entity.ContactMessage> findAllByOrderByCreatedAtDesc() {
            return new ArrayList<>(store);
        }

        @Override
        public List<com.vpnexues.svc.entity.ContactMessage> findByStatusOrderByCreatedAtDesc(
                com.vpnexues.svc.entity.ContactMessageStatus status) {
            return store.stream().filter(m -> m.getStatus() == status).toList();
        }

        @Override
        public List<com.vpnexues.svc.entity.ContactMessage> findByChatSessionId(UUID sessionId) {
            return store.stream().filter(m -> sessionId.equals(m.getChatSessionId())).toList();
        }

        @Override
        public Optional<com.vpnexues.svc.entity.ContactMessage> findById(UUID id) {
            return store.stream().filter(m -> id.equals(m.getId())).findFirst();
        }

        @Override
        public boolean existsById(UUID id) {
            return store.stream().anyMatch(m -> id.equals(m.getId()));
        }

        @Override
        public List<com.vpnexues.svc.entity.ContactMessage> findAll() {
            return new ArrayList<>(store);
        }

        @Override
        public List<com.vpnexues.svc.entity.ContactMessage> findAll(Sort sort) {
            return findAll();
        }

        @Override
        public List<com.vpnexues.svc.entity.ContactMessage> findAllById(Iterable<UUID> ids) {
            List<com.vpnexues.svc.entity.ContactMessage> out = new ArrayList<>();
            ids.forEach(id -> findById(id).ifPresent(out::add));
            return out;
        }

        @Override
        public Page<com.vpnexues.svc.entity.ContactMessage> findAll(Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends com.vpnexues.svc.entity.ContactMessage> List<S> saveAll(Iterable<S> entities) {
            List<S> out = new ArrayList<>();
            entities.forEach(e -> out.add(save(e)));
            return out;
        }

        @Override
        public <S extends com.vpnexues.svc.entity.ContactMessage> Optional<S> findOne(Example<S> example) {
            return Optional.empty();
        }

        @Override
        public <S extends com.vpnexues.svc.entity.ContactMessage> List<S> findAll(Example<S> example) {
            return List.of();
        }

        @Override
        public <S extends com.vpnexues.svc.entity.ContactMessage> List<S> findAll(Example<S> example, Sort sort) {
            return List.of();
        }

        @Override
        public <S extends com.vpnexues.svc.entity.ContactMessage> Page<S> findAll(Example<S> example, Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends com.vpnexues.svc.entity.ContactMessage> long count(Example<S> example) {
            return 0;
        }

        @Override
        public <S extends com.vpnexues.svc.entity.ContactMessage> boolean exists(Example<S> example) {
            return false;
        }

        @Override
        public <S extends com.vpnexues.svc.entity.ContactMessage, R> R findBy(
                Example<S> example, java.util.function.Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public void deleteById(UUID id) {
            store.removeIf(m -> id.equals(m.getId()));
        }

        @Override
        public void delete(com.vpnexues.svc.entity.ContactMessage entity) {
            store.remove(entity);
        }

        @Override
        public void deleteAllById(Iterable<? extends UUID> ids) {
            ids.forEach(this::deleteById);
        }

        @Override
        public void deleteAll(Iterable<? extends com.vpnexues.svc.entity.ContactMessage> entities) {
            entities.forEach(store::remove);
        }

        @Override
        public void deleteAll() {
            store.clear();
        }

        @Override
        public void flush() {
        }

        @Override
        public <S extends com.vpnexues.svc.entity.ContactMessage> S saveAndFlush(S entity) {
            return save(entity);
        }

        @Override
        public <S extends com.vpnexues.svc.entity.ContactMessage> List<S> saveAllAndFlush(Iterable<S> entities) {
            return saveAll(entities);
        }

        @Override
        public void deleteAllInBatch(Iterable<com.vpnexues.svc.entity.ContactMessage> entities) {
            deleteAll(entities);
        }

        @Override
        public void deleteAllByIdInBatch(Iterable<UUID> ids) {
            deleteAllById(ids);
        }

        @Override
        public void deleteAllInBatch() {
            store.clear();
        }

        @Override
        public com.vpnexues.svc.entity.ContactMessage getReferenceById(UUID id) {
            return findById(id).orElse(null);
        }

        @Override
        @SuppressWarnings("deprecation")
        public com.vpnexues.svc.entity.ContactMessage getById(UUID id) {
            return findById(id).orElse(null);
        }

        @Override
        @SuppressWarnings("deprecation")
        public com.vpnexues.svc.entity.ContactMessage getOne(UUID id) {
            return findById(id).orElse(null);
        }
    }

    /** ContactFormMailService without mail infra — fail-open notify path. */
    static class NoopMailService extends ContactFormMailService {
        NoopMailService() {
            super(null);
        }

        @Override
        public boolean sendChatTranscriptEmail(
                String visitorName, String visitorEmail, String subject, List<String> transcriptLines) {
            return true;
        }
    }
}
