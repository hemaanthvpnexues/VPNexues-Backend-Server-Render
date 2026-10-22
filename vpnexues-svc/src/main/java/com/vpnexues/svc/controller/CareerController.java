package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.CareerJobDto;
import com.vpnexues.svc.dto.CreateCareerApplicationRequest;
import com.vpnexues.svc.service.CareerApplicationService;
import com.vpnexues.svc.service.CareerJobService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public careers endpoints (no auth — SecurityConfig permitAll's /api/careers/**):
 * the open positions backing the /careers page, and the application submission
 * that emails HR.
 */
@RestController
@RequiredArgsConstructor
public class CareerController {

    private final CareerApplicationService careerApplicationService;
    private final CareerJobService careerJobService;

    /** Active jobs only — the sub-admin "Visible on the public careers page" flag is honoured here. */
    @GetMapping("/api/careers/jobs")
    public List<CareerJobDto> jobs() {
        return careerJobService.listActive();
    }

    @PostMapping("/api/careers/apply")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void apply(@RequestBody @Valid CreateCareerApplicationRequest req) {
        careerApplicationService.sendApplicationEmail(req);
    }
}
