package com.vpnexues.svc.controller;

import com.vpnexues.svc.dto.CreateTestimonialRequest;
import com.vpnexues.svc.dto.TestimonialDto;
import com.vpnexues.svc.dto.UpdateTestimonialRequest;
import com.vpnexues.svc.service.TestimonialService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminTestimonialController {

    private final TestimonialService testimonialService;

    @GetMapping("/api/admin/testimonials")
    public List<TestimonialDto> list() {
        return testimonialService.listAll();
    }

    @PostMapping("/api/admin/testimonials")
    @ResponseStatus(HttpStatus.CREATED)
    public TestimonialDto create(@RequestBody @Valid CreateTestimonialRequest req) {
        return testimonialService.create(req);
    }

    @PutMapping("/api/admin/testimonials/{id}")
    public TestimonialDto update(@PathVariable UUID id, @RequestBody @Valid UpdateTestimonialRequest req) {
        return testimonialService.update(id, req);
    }

    @DeleteMapping("/api/admin/testimonials/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        testimonialService.delete(id);
    }
}
