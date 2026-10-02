package com.vpnexues.svc.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TeamMemberDto(
        UUID id, String slug, String name, String role, String photoUrl, String description, boolean isDirector, int displayOrder, boolean active,
        List<ResponsibilityItem> responsibilities, List<String> keySkills, Instant createdAt) {
}
