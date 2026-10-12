package com.vpnexues.svc.dto;

import java.util.UUID;

public record GalleryImageDto(
        UUID id,
        String imageUrl,
        String caption,
        String altText,
        int displayOrder,
        boolean active) {
}
