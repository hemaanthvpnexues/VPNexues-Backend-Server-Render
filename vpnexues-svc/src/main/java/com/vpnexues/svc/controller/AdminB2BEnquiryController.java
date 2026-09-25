package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.B2BEnquiryDto;
import com.vpnexues.svc.dto.UpdateB2BEnquiryStatusRequest;
import com.vpnexues.svc.entity.B2BEnquiryStatus;
import com.vpnexues.svc.service.B2BEnquiryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminB2BEnquiryController {

    private final B2BEnquiryService b2bEnquiryService;

    @GetMapping("/api/admin/b2b-enquiries")
    public List<B2BEnquiryDto> list(@RequestParam(required = false) B2BEnquiryStatus status) {
        return b2bEnquiryService.list(status);
    }

    @PutMapping("/api/admin/b2b-enquiries/{id}/status")
    public B2BEnquiryDto updateStatus(@PathVariable UUID id, @RequestBody @Valid UpdateB2BEnquiryStatusRequest req) {
        return b2bEnquiryService.updateStatus(id, req.status());
    }
}
