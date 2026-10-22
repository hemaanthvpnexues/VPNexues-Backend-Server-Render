package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.CareerJobDto;
import com.vpnexues.svc.dto.CareerJobOpeningDto;
import com.vpnexues.svc.dto.CreateCareerJobRequest;
import com.vpnexues.svc.dto.UpdateCareerJobRequest;
import com.vpnexues.svc.entity.CareerJob;
import com.vpnexues.svc.exception.BadRequestException;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.CareerJobRepository;
import com.vpnexues.svc.util.Slugify;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional
public class CareerJobService {

    private final CareerJobRepository careerJobRepository;
    private final ObjectMapper objectMapper;

    /** Admin list: all jobs including inactive. */
    @Transactional(readOnly = true)
    public List<CareerJobDto> listAll() {
        return careerJobRepository.findAllByOrderByDisplayOrderAscCreatedAtAsc().stream()
                .map(this::toDto)
                .toList();
    }

    /** Public /careers list: active jobs only, in display order. */
    @Transactional(readOnly = true)
    public List<CareerJobDto> listActive() {
        return careerJobRepository.findByActiveTrueOrderByDisplayOrderAscCreatedAtAsc().stream()
                .map(this::toDto)
                .toList();
    }

    public CareerJobDto create(CreateCareerJobRequest req) {
        CareerJob job = new CareerJob();
        // slug is generated here and never touched again: /careers/apply?job=<slug>
        // links and the job translations are keyed on it.
        job.setSlug(uniqueSlug(req.title()));
        apply(job, req.title(), req.type(), req.location(), req.description(), req.skills());
        job.setIcon(trimOrNull(req.icon()));
        job.setDuration(trimOrNull(req.duration()));
        job.setStipend(trimOrNull(req.stipend()));
        job.setStartDate(trimOrNull(req.startDate()));
        job.setAbout(trimOrNull(req.about()));
        job.setResponsibilities(serializeList(req.responsibilities()));
        job.setRequirements(serializeList(req.requirements()));
        job.setPerks(serializeList(req.perks()));
        job.setOpenings(serializeOpenings(req.openings()));
        job.setActive(req.active() == null || req.active());
        job.setDisplayOrder(req.displayOrder() == null ? nextOrder() : req.displayOrder());
        return toDto(careerJobRepository.save(job));
    }

    public CareerJobDto update(UUID id, UpdateCareerJobRequest req) {
        CareerJob job = getEntity(id);
        // slug stays as-is: the public apply URL must not break on a rename.
        apply(job, req.title(), req.type(), req.location(), req.description(), req.skills());
        job.setIcon(trimOrNull(req.icon()));
        job.setDuration(trimOrNull(req.duration()));
        job.setStipend(trimOrNull(req.stipend()));
        job.setStartDate(trimOrNull(req.startDate()));
        job.setAbout(trimOrNull(req.about()));
        // null keeps stored detail lists; a supplied (possibly empty) list replaces them.
        if (req.responsibilities() != null) {
            job.setResponsibilities(serializeList(req.responsibilities()));
        }
        if (req.requirements() != null) {
            job.setRequirements(serializeList(req.requirements()));
        }
        if (req.perks() != null) {
            job.setPerks(serializeList(req.perks()));
        }
        if (req.openings() != null) {
            job.setOpenings(serializeOpenings(req.openings()));
        }
        if (req.active() != null) {
            job.setActive(req.active());
        }
        if (req.displayOrder() != null) {
            job.setDisplayOrder(req.displayOrder());
        }
        return toDto(careerJobRepository.save(job));
    }

    public void delete(UUID id) {
        if (!careerJobRepository.existsById(id)) {
            throw new NotFoundException("Career job not found: " + id);
        }
        careerJobRepository.deleteById(id);
    }

    private void apply(CareerJob job, String title, String type, String location, String description, String skills) {
        job.setTitle(title.trim());
        job.setType(type.trim());
        job.setLocation(location.trim());
        job.setDescription(description.trim());
        job.setSkills(skills == null ? "" : skills.trim());
    }

    /** Slugify on create; on the (rare) duplicate, append -2, -3, ... until unique. */
    private String uniqueSlug(String title) {
        String base = Slugify.slugify(title);
        if (base.isEmpty()) {
            base = "job";
        }
        String slug = base;
        int suffix = 2;
        while (careerJobRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    private int nextOrder() {
        List<CareerJob> all = careerJobRepository.findAllByOrderByDisplayOrderAscCreatedAtAsc();
        return all.isEmpty() ? 0 : all.get(all.size() - 1).getDisplayOrder() + 1;
    }

    private CareerJob getEntity(UUID id) {
        return careerJobRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Career job not found: " + id));
    }

    /** null in → null out; blank items are dropped. */
    private String serializeList(List<String> items) {
        if (items == null) {
            return null;
        }
        List<String> clean = items.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .toList();
        try {
            return objectMapper.writeValueAsString(clean);
        } catch (JacksonException ex) {
            throw new BadRequestException("Job details could not be saved.");
        }
    }

    /** null in → null out (single-role job); openings without a role are dropped. */
    private String serializeOpenings(List<CareerJobOpeningDto> openings) {
        if (openings == null) {
            return null;
        }
        List<CareerJobOpeningDto> clean = openings.stream()
                .filter(opening -> opening != null && opening.role() != null && !opening.role().isBlank())
                .map(opening -> new CareerJobOpeningDto(
                        opening.role().trim(),
                        trimOrNull(opening.type()),
                        sanitizeList(opening.responsibilities())))
                .toList();
        try {
            return objectMapper.writeValueAsString(clean);
        } catch (JacksonException ex) {
            throw new BadRequestException("Job openings could not be saved.");
        }
    }

    private List<String> sanitizeList(List<String> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .toList();
    }

    private List<String> deserializeList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return List.of(objectMapper.readValue(json, String[].class));
        } catch (JacksonException ex) {
            return List.of();
        }
    }

    private List<CareerJobOpeningDto> deserializeOpenings(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return List.of(objectMapper.readValue(json, CareerJobOpeningDto[].class));
        } catch (JacksonException ex) {
            return List.of();
        }
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private CareerJobDto toDto(CareerJob j) {
        return new CareerJobDto(
                j.getId(),
                j.getSlug(),
                j.getTitle(),
                j.getType(),
                j.getLocation(),
                j.getIcon(),
                j.getDescription(),
                j.getSkills(),
                j.getDuration(),
                j.getStipend(),
                j.getStartDate(),
                j.getAbout(),
                deserializeList(j.getResponsibilities()),
                deserializeList(j.getRequirements()),
                deserializeList(j.getPerks()),
                deserializeOpenings(j.getOpenings()),
                j.isActive(),
                j.getDisplayOrder(),
                j.getCreatedAt());
    }
}
