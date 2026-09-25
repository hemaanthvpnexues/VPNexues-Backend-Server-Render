package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.B2BEnquiryDto;
import com.vpnexues.svc.dto.CreateB2BEnquiryRequest;
import com.vpnexues.svc.service.B2BEnquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public submission endpoint — no auth (see SecurityConfig, already permitAll's this path). */
@RestController
@RequiredArgsConstructor
public class B2BEnquiryController {

    private final B2BEnquiryService b2bEnquiryService;

    @PostMapping("/api/b2b-enquiries")
    @ResponseStatus(HttpStatus.CREATED)
    public B2BEnquiryDto create(@RequestBody @Valid CreateB2BEnquiryRequest req) {
        return b2bEnquiryService.create(req);
    }
}
