package com.vpnexues.svc.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreateNewsArticleRequest(
        @NotBlank String title,
        String author,
        @NotBlank String imageUrl,
        String excerpt,
        @NotBlank String content,
        String tags,
        LocalDate publishedDate,
        String readTime,
        Integer displayOrder,
        String category,
        Boolean active) {
}
