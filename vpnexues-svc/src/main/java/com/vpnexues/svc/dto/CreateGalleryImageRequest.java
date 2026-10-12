package com.vpnexues.svc.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateGalleryImageRequest(
        @NotBlank String imageUrl,
        String caption,
        String altText,
        Integer displayOrder,
        Boolean active) {
}
