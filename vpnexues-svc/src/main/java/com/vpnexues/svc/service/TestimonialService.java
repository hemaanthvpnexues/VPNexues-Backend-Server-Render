package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.CreateTestimonialRequest;
import com.vpnexues.svc.dto.TestimonialDto;
import com.vpnexues.svc.dto.UpdateTestimonialRequest;
import com.vpnexues.svc.entity.Testimonial;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.TestimonialRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TestimonialService {

    private final TestimonialRepository testimonialRepository;

    @Transactional(readOnly = true)
    public List<TestimonialDto> listActive() {
        return testimonialRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TestimonialDto> listAll() {
        return testimonialRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    public TestimonialDto create(CreateTestimonialRequest req) {
        Testimonial testimonial = new Testimonial();
        testimonial.setName(req.name());
        testimonial.setRole(req.role());
        testimonial.setLocation(req.location());
        testimonial.setRating((short) req.rating());
        testimonial.setQuote(req.quote());
        testimonial.setPhotoUrl(req.photoUrl());
        return toDto(testimonialRepository.save(testimonial));
    }

    public TestimonialDto update(UUID id, UpdateTestimonialRequest req) {
        Testimonial testimonial = getEntity(id);
        testimonial.setName(req.name());
        testimonial.setRole(req.role());
        testimonial.setLocation(req.location());
        testimonial.setRating((short) req.rating());
        testimonial.setQuote(req.quote());
        testimonial.setPhotoUrl(req.photoUrl());
        testimonial.setActive(req.active());
        return toDto(testimonialRepository.save(testimonial));
    }

    public void delete(UUID id) {
        if (!testimonialRepository.existsById(id)) {
            throw new NotFoundException("Testimonial not found: " + id);
        }
        testimonialRepository.deleteById(id);
    }

    private Testimonial getEntity(UUID id) {
        return testimonialRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Testimonial not found: " + id));
    }

    private TestimonialDto toDto(Testimonial t) {
        return new TestimonialDto(
                t.getId(), t.getName(), t.getRole(), t.getLocation(), t.getRating(), t.getQuote(), t.getPhotoUrl(), t.isActive());
    }
}
