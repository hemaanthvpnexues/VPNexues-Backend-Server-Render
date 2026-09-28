package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.ContactMessageDto;
import com.vpnexues.svc.dto.CreateContactMessageRequest;
import com.vpnexues.svc.entity.ContactMessage;
import com.vpnexues.svc.entity.ContactMessageStatus;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.ContactMessageRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;
    private final ContactFormMailService contactFormMailService;

    public ContactMessageDto create(CreateContactMessageRequest req) {
        String name = req.name().trim();
        String email = req.email().trim();
        String phone = req.phone() == null ? null : req.phone().trim();
        String countryCode = req.countryCode() == null ? null : req.countryCode().trim();
        String subject = req.subject().trim();
        String message = req.message().trim();
        ContactMessage entity = new ContactMessage();
        entity.setName(name);
        entity.setEmail(email);
        entity.setPhone(phone);
        entity.setCountryCode(countryCode);
        entity.setSubject(subject);
        entity.setMessage(message);
        ContactMessageDto saved = toDto(contactMessageRepository.save(entity));
        // Fail-open: the message is persisted above; a mail outage must not fail the request.
        contactFormMailService.sendContactEmail(name, email, phone, subject, message);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ContactMessageDto> list(ContactMessageStatus status) {
        List<ContactMessage> messages = status == null
                ? contactMessageRepository.findAllByOrderByCreatedAtDesc()
                : contactMessageRepository.findByStatusOrderByCreatedAtDesc(status);
        return messages.stream().map(this::toDto).toList();
    }

    /** Matches the admin UI's "open to view" flow — fetching the detail marks a NEW message as READ. */
    public ContactMessageDto getAndMarkRead(UUID id) {
        ContactMessage message = getEntity(id);
        if (message.getStatus() == ContactMessageStatus.NEW) {
            message.setStatus(ContactMessageStatus.READ);
            contactMessageRepository.save(message);
        }
        return toDto(message);
    }

    public ContactMessageDto updateStatus(UUID id, ContactMessageStatus status) {
        ContactMessage message = getEntity(id);
        message.setStatus(status);
        return toDto(contactMessageRepository.save(message));
    }

    public void delete(UUID id) {
        if (!contactMessageRepository.existsById(id)) {
            throw new NotFoundException("Contact message not found: " + id);
        }
        contactMessageRepository.deleteById(id);
    }

    private ContactMessage getEntity(UUID id) {
        return contactMessageRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Contact message not found: " + id));
    }

    private ContactMessageDto toDto(ContactMessage m) {
        return new ContactMessageDto(
                m.getId(),
                m.getName(),
                m.getEmail(),
                m.getPhone(),
                m.getCountryCode(),
                m.getSubject(),
                m.getMessage(),
                m.getStatus().name(),
                m.getCreatedAt(),
                m.getChatSessionId());
    }
}
