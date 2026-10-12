package com.vpnexues.svc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vpnexues.svc.dto.CreateGalleryImageRequest;
import com.vpnexues.svc.dto.GalleryImageDto;
import com.vpnexues.svc.dto.UpdateGalleryImageRequest;
import com.vpnexues.svc.entity.GalleryImage;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.GalleryImageRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for gallery defaults and the null-keeps-stored-value update rules. */
class GalleryServiceTest {

    private GalleryImageRepository repository;
    private GalleryService service;

    @BeforeEach
    void setUp() {
        repository = mock(GalleryImageRepository.class);
        when(repository.save(any(GalleryImage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of());
        service = new GalleryService(repository);
    }

    @Test
    void createFillsDefaultsForOptionalFields() {
        GalleryImageDto dto = service.create(new CreateGalleryImageRequest(
                "  https://img/x.jpg  ", "  Sunrise over the fields  ", "", null, null));

        assertEquals("https://img/x.jpg", dto.imageUrl(), "imageUrl is trimmed");
        assertEquals("Sunrise over the fields", dto.caption());
        assertNull(dto.altText(), "blank alt text is stored as null");
        assertEquals(0, dto.displayOrder(), "an empty list appends at order 0");
        assertTrue(dto.active(), "a null active flag defaults to visible");
    }

    @Test
    void updateKeepsStoredOrderAndVisibilityWhenNull() {
        GalleryImage existing = new GalleryImage();
        existing.setId(UUID.randomUUID());
        existing.setDisplayOrder(7);
        existing.setActive(false);
        UUID id = existing.getId();
        when(repository.findById(id)).thenReturn(Optional.of(existing));

        GalleryImageDto dto = service.update(id, new UpdateGalleryImageRequest(
                "https://img/new.jpg", null, null, null, null));

        assertEquals(7, dto.displayOrder(), "null displayOrder keeps the stored order");
        assertFalse(dto.active(), "null active keeps the stored flag");
        assertNull(dto.caption(), "null caption clears the stored caption");
    }

    @Test
    void updateReordersAndCanHide() {
        GalleryImage existing = new GalleryImage();
        existing.setId(UUID.randomUUID());
        existing.setDisplayOrder(3);
        UUID id = existing.getId();
        when(repository.findById(id)).thenReturn(Optional.of(existing));

        GalleryImageDto dto = service.update(id, new UpdateGalleryImageRequest(
                "https://img/y.jpg", "Caption", "alt", 12, false));

        assertEquals(12, dto.displayOrder());
        assertFalse(dto.active());
        assertEquals("Caption", dto.caption());
    }

    @Test
    void deleteThrowsNotFoundForUnknownId() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> service.delete(id));
    }
}
