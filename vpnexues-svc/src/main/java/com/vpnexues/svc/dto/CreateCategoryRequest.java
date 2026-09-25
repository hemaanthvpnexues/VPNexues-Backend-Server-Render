package com.vpnexues.svc.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(@NotBlank String name, String slug, String emoji) {
}
