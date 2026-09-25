package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.ContactMessageDto;
import com.vpnexues.svc.dto.CreateContactMessageRequest;
import com.vpnexues.svc.service.ContactMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public submission endpoint — backs the Phase 4 Contact page's real form. */
@RestController
@RequiredArgsConstructor
public class ContactMessageController {

    private final ContactMessageService contactMessageService;

    @PostMapping("/api/contact-messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ContactMessageDto create(@RequestBody @Valid CreateContactMessageRequest req) {
        return contactMessageService.create(req);
    }
}
