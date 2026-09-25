package com.vpnexues.svc.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCategoryRequest(@NotBlank String name, String emoji, boolean active) {
}
