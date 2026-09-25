package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.TestimonialDto;
import com.vpnexues.svc.service.TestimonialService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public read-only endpoint for the Phase 4 marketing testimonial page — active testimonials only. */
@RestController
@RequiredArgsConstructor
public class TestimonialController {

    private final TestimonialService testimonialService;

    @GetMapping("/api/testimonials")
    public List<TestimonialDto> list() {
        return testimonialService.listActive();
    }
}
