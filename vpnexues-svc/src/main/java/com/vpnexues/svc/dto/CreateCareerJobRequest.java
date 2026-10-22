package com.vpnexues.svc.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * {@code slug} is derived from the title on create (never sent by the client) and
 * frozen afterwards, so shared /careers/apply links keep working.
 */
public record CreateCareerJobRequest(
        @NotBlank String title,
        @NotBlank String type,
        @NotBlank String location,
        @NotBlank String description,
        String skills,
        String icon,
        String duration,
        String stipend,
        String startDate,
        String about,
        List<String> responsibilities,
        List<String> requirements,
        List<String> perks,
        List<CareerJobOpeningDto> openings,
        Boolean active,
        Integer displayOrder) {
}
