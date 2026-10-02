package com.vpnexues.svc.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/** Full replace of the editable fields; legacyKey / comments / displayOrder are not editable. */
public record UpdateNewsArticleRequest(
        @NotBlank String title,
        String author,
        @NotBlank String imageUrl,
        String excerpt,
        @NotBlank String content,
        String tags,
        LocalDate publishedDate,
        String readTime,
        String category,
        Boolean active) {
}
