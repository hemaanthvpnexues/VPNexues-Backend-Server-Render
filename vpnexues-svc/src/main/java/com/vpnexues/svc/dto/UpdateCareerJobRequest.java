package com.vpnexues.svc.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** {@code responsibilities}/{@code requirements}/{@code perks}/{@code openings}: null keeps the stored value, [] clears it. */
public record UpdateCareerJobRequest(
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
