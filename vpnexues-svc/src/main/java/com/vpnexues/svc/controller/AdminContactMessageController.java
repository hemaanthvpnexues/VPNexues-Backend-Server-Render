package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.ContactMessageDto;
import com.vpnexues.svc.dto.UpdateContactMessageStatusRequest;
import com.vpnexues.svc.entity.ContactMessageStatus;
import com.vpnexues.svc.service.ContactMessageService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminContactMessageController {

    private final ContactMessageService contactMessageService;

    @GetMapping("/api/admin/contact-messages")
    public List<ContactMessageDto> list(@RequestParam(required = false) ContactMessageStatus status) {
        return contactMessageService.list(status);
    }

    @GetMapping("/api/admin/contact-messages/{id}")
    public ContactMessageDto get(@PathVariable UUID id) {
        return contactMessageService.getAndMarkRead(id);
    }

    @PutMapping("/api/admin/contact-messages/{id}/status")
    public ContactMessageDto updateStatus(
            @PathVariable UUID id, @RequestBody @Valid UpdateContactMessageStatusRequest req) {
        return contactMessageService.updateStatus(id, req.status());
    }

    @DeleteMapping("/api/admin/contact-messages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        contactMessageService.delete(id);
    }
}
