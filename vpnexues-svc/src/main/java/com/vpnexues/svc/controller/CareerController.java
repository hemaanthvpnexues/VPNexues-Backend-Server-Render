package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.CreateCareerApplicationRequest;
import com.vpnexues.svc.service.CareerApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public submission endpoint — sends job application email to HR. */
@RestController
@RequiredArgsConstructor
public class CareerController {

    private final CareerApplicationService careerApplicationService;

    @PostMapping("/api/careers/apply")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void apply(@RequestBody @Valid CreateCareerApplicationRequest req) {
        careerApplicationService.sendApplicationEmail(req);
    }
}
