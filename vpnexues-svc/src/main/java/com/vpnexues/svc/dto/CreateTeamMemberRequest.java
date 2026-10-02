package com.vpnexues.svc.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateTeamMemberRequest(
        @NotBlank String name,
        String role,
        String photoUrl,
        String description,
        Boolean isDirector,
        Integer displayOrder,
        Boolean active,
        List<ResponsibilityItem> responsibilities,
        List<String> keySkills) {
}
