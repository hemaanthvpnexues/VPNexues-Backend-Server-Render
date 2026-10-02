package com.vpnexues.svc.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** {@code responsibilities}/{@code keySkills}: null keeps the stored value, [] clears it. */
public record UpdateTeamMemberRequest(
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
