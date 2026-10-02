package com.vpnexues.svc.dto;

import java.time.LocalDate;
import java.util.UUID;

public record NewsArticleDto(
        UUID id,
        String legacyKey,
        String title,
        String author,
        String imageUrl,
        String excerpt,
        String content,
        String tags,
        LocalDate publishedDate,
        String readTime,
        int comments,
        int displayOrder,
        boolean active,
        String category) {
}
