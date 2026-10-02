package com.vpnexues.svc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A public /news article. {@code legacyKey} links a seeded article to its rich static layout. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "news_articles")
public class NewsArticle extends BaseEntity {

    @Column(name = "legacy_key", unique = true)
    private String legacyKey;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(name = "image_url", nullable = false, columnDefinition = "text")
    private String imageUrl;

    @Column(columnDefinition = "text")
    private String excerpt;

    /** Paragraphs separated by blank lines — public pages split on \n\n. */
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    /** Comma-separated tag list (matches the static site's filter chips). */
    private String tags;

    @Column(name = "published_date", nullable = false)
    private LocalDate publishedDate;

    @Column(name = "read_time", nullable = false)
    private String readTime = "3 min";

    @Column(nullable = false)
    private int comments;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /** Inactive articles are hidden from public /news and its detail page. */
    @Column(nullable = false)
    private boolean active = true;

    /** Free-form admin label shown as a chip on /news (e.g. Organic, Farm, Fresh). */
    @Column(length = 100)
    private String category;
}
