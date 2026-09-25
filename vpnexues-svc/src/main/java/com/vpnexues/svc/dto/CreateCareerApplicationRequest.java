package com.vpnexues.svc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateCareerApplicationRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        String phone,
        String location,
        @NotBlank String role,
        String linkedin,
        String github,
        String resume,
        String availability,
        String source,
        String whyJoin,
        String timestamp) {
}
