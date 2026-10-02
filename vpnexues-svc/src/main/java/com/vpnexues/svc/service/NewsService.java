package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.CreateNewsArticleRequest;
import com.vpnexues.svc.dto.NewsArticleDto;
import com.vpnexues.svc.dto.UpdateNewsArticleRequest;
import com.vpnexues.svc.entity.NewsArticle;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.NewsArticleRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NewsService {

    private final NewsArticleRepository newsArticleRepository;

    /** Public /news order: active only, newest first, displayOrder as tie-break (matches the static site). */
    @Transactional(readOnly = true)
    public List<NewsArticleDto> listPublished() {
        return newsArticleRepository.findByActiveTrueOrderByPublishedDateDescDisplayOrderAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NewsArticleDto> listAll() {
        return newsArticleRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public NewsArticleDto getById(UUID id) {
        return toDto(getEntity(id));
    }

    /** Public detail: an inactive article reads as "not found" so it can't be opened by URL. */
    @Transactional(readOnly = true)
    public NewsArticleDto getPublishedById(UUID id) {
        NewsArticle article = getEntity(id);
        if (!article.isActive()) {
            throw new NotFoundException("News article not found: " + id);
        }
        return toDto(article);
    }

    public NewsArticleDto create(CreateNewsArticleRequest req) {
        NewsArticle article = new NewsArticle();
        article.setTitle(req.title().trim());
        article.setAuthor(req.author() == null || req.author().isBlank() ? "Admin" : req.author().trim());
        article.setImageUrl(req.imageUrl().trim());
        article.setExcerpt(req.excerpt());
        article.setContent(req.content());
        article.setTags(normalizeTags(req.tags()));
        article.setPublishedDate(req.publishedDate() != null ? req.publishedDate() : LocalDate.now());
        article.setReadTime(req.readTime() == null || req.readTime().isBlank() ? "3 min" : req.readTime().trim());
        article.setDisplayOrder(req.displayOrder() != null ? req.displayOrder() : nextOrder());
        article.setCategory(normalizeTags(req.category()));
        article.setActive(req.active() == null || req.active());
        return toDto(newsArticleRepository.save(article));
    }

    public NewsArticleDto update(UUID id, UpdateNewsArticleRequest req) {
        NewsArticle article = getEntity(id);
        // legacyKey is intentionally never updated: it links the row to its rich static layout.
        article.setTitle(req.title().trim());
        article.setAuthor(req.author() == null || req.author().isBlank() ? "Admin" : req.author().trim());
        article.setImageUrl(req.imageUrl().trim());
        article.setExcerpt(req.excerpt());
        article.setContent(req.content());
        article.setTags(normalizeTags(req.tags()));
        if (req.publishedDate() != null) {
            article.setPublishedDate(req.publishedDate());
        }
        if (req.readTime() != null && !req.readTime().isBlank()) {
            article.setReadTime(req.readTime().trim());
        }
        article.setCategory(normalizeTags(req.category()));
        // null keeps the stored flag so an old client can't silently unpublish articles.
        if (req.active() != null) {
            article.setActive(req.active());
        }
        return toDto(newsArticleRepository.save(article));
    }

    public void delete(UUID id) {
        if (!newsArticleRepository.existsById(id)) {
            throw new NotFoundException("News article not found: " + id);
        }
        newsArticleRepository.deleteById(id);
    }

    /** Accepts comma-separated input from the admin form; stored trimmed, null when empty. */
    private String normalizeTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return null;
        }
        return tags.trim();
    }

    private int nextOrder() {
        List<NewsArticle> all = newsArticleRepository.findAllByOrderByDisplayOrderAsc();
        return all.isEmpty() ? 0 : all.get(all.size() - 1).getDisplayOrder() + 1;
    }

    private NewsArticle getEntity(UUID id) {
        return newsArticleRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("News article not found: " + id));
    }

    private NewsArticleDto toDto(NewsArticle a) {
        return new NewsArticleDto(
                a.getId(),
                a.getLegacyKey(),
                a.getTitle(),
                a.getAuthor(),
                a.getImageUrl(),
                a.getExcerpt(),
                a.getContent(),
                a.getTags(),
                a.getPublishedDate(),
                a.getReadTime(),
                a.getComments(),
                a.getDisplayOrder(),
                a.isActive(),
                a.getCategory());
    }
}
