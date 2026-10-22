package com.vpnexues.svc.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A careers job as both the sub-admin dashboard and the public /careers page see it.
 *
 * <p>The detail lists come back as real lists (the service does the JSON conversion),
 * so callers never touch the stored TEXT columns.
 */
public record CareerJobDto(
        UUID id,
        String slug,
        String title,
        String type,
        String location,
        String icon,
        String description,
        String skills,
        String duration,
        String stipend,
        String startDate,
        String about,
        List<String> responsibilities,
        List<String> requirements,
        List<String> perks,
        List<CareerJobOpeningDto> openings,
        boolean active,
        int displayOrder,
        Instant createdAt) {
}
