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

    public ContactMessageDto create(CreateContactMessageRequest req) {
        ContactMessage message = new ContactMessage();
        message.setName(req.name());
        message.setEmail(req.email());
        message.setPhone(req.phone());
        message.setCountryCode(req.countryCode());
        message.setSubject(req.subject());
        message.setMessage(req.message());
        return toDto(contactMessageRepository.save(message));
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
                m.getCreatedAt());
    }
}
