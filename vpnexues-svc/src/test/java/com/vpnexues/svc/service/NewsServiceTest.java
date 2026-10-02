package com.vpnexues.svc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vpnexues.svc.dto.CreateNewsArticleRequest;
import com.vpnexues.svc.dto.NewsArticleDto;
import com.vpnexues.svc.dto.UpdateNewsArticleRequest;
import com.vpnexues.svc.entity.NewsArticle;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.NewsArticleRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for news defaults and the immutability of the rich-layout linkage. */
class NewsServiceTest {

    private NewsArticleRepository repository;
    private NewsService service;

    @BeforeEach
    void setUp() {
        repository = mock(NewsArticleRepository.class);
        when(repository.save(any(NewsArticle.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of());
        service = new NewsService(repository);
    }

    @Test
    void createFillsDefaultsForOptionalFields() {
        NewsArticleDto dto = service.create(new CreateNewsArticleRequest(
                "  Fresh News  ", "", "https://img/x.jpg", null, "Para one.\n\nPara two.", "  ", null, null, null, null, null));

        assertEquals("Fresh News", dto.title());
        assertEquals("Admin", dto.author());
        assertEquals("3 min", dto.readTime());
        assertEquals(LocalDate.now(), dto.publishedDate());
        assertNull(dto.tags(), "blank tag input is stored as null");
        assertEquals(0, dto.displayOrder());
        assertTrue(dto.active(), "a null active flag defaults to published");
        assertNull(dto.category());
        assertEquals(2, dto.content().split("\n\n", -1).length, "paragraphs are stored verbatim");
    }

    @Test
    void updatePreservesLegacyKeyCommentsAndOrder() {
        NewsArticle existing = new NewsArticle();
        existing.setId(UUID.randomUUID());
        existing.setLegacyKey("3");
        existing.setComments(2);
        existing.setDisplayOrder(5);
        existing.setPublishedDate(LocalDate.of(2026, 5, 15));
        existing.setReadTime("3 min");
        UUID id = existing.getId();
        when(repository.findById(id)).thenReturn(Optional.of(existing));

        NewsArticleDto dto = service.update(id, new UpdateNewsArticleRequest(
                "Edited Title", "New Author", "https://img/new.jpg", "excerpt", "Only body.", "Spices", null, "", null, null));

        assertEquals("3", dto.legacyKey(), "legacyKey links the row to its static layout and must never change");
        assertEquals(2, dto.comments());
        assertEquals(5, dto.displayOrder());
        assertEquals(LocalDate.of(2026, 5, 15), dto.publishedDate(), "null publishedDate keeps the existing date");
        assertEquals("3 min", dto.readTime(), "blank readTime keeps the existing value");
        assertEquals("Edited Title", dto.title());
        assertTrue(dto.active(), "null active on update keeps the stored flag");
    }

    @Test
    void updateReplacesCategoryAndCanUnpublish() {
        NewsArticle existing = new NewsArticle();
        existing.setId(UUID.randomUUID());
        existing.setLegacyKey("2");
        existing.setCategory("Old");
        UUID id = existing.getId();
        when(repository.findById(id)).thenReturn(Optional.of(existing));

        NewsArticleDto dto = service.update(id, new UpdateNewsArticleRequest(
                "T", "A", "https://img/y.jpg", null, "Body.", null, null, null, "  Organic, Farm  ", false));

        assertEquals("Organic, Farm", dto.category(), "category is trimmed on save");
        assertEquals(false, dto.active());
    }

    @Test
    void deleteThrowsNotFoundForUnknownId() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> service.delete(id));
    }
}
