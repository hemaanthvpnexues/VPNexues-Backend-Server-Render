package com.vpnexues.svc.dto;

import jakarta.validation.constraints.NotBlank;

/** Full replace of the editable fields; null displayOrder / active keep the stored values. */
public record UpdateGalleryImageRequest(
        @NotBlank String imageUrl,
        String caption,
        String altText,
        Integer displayOrder,
        Boolean active) {
}
