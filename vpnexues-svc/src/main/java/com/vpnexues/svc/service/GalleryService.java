package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.CreateGalleryImageRequest;
import com.vpnexues.svc.dto.GalleryImageDto;
import com.vpnexues.svc.dto.UpdateGalleryImageRequest;
import com.vpnexues.svc.entity.GalleryImage;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.GalleryImageRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class GalleryService {

    private final GalleryImageRepository galleryImageRepository;

    /** Public /gallery order: active only, by displayOrder (matches the static site's grid order). */
    @Transactional(readOnly = true)
    public List<GalleryImageDto> listActive() {
        return galleryImageRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GalleryImageDto> listAll() {
        return galleryImageRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::toDto)
                .toList();
    }

    public GalleryImageDto create(CreateGalleryImageRequest req) {
        GalleryImage image = new GalleryImage();
        image.setImageUrl(req.imageUrl().trim());
        image.setCaption(normalizeCaption(req.caption()));
        image.setAltText(normalizeCaption(req.altText()));
        image.setDisplayOrder(req.displayOrder() != null ? req.displayOrder() : nextOrder());
        image.setActive(req.active() == null || req.active());
        return toDto(galleryImageRepository.save(image));
    }

    public GalleryImageDto update(UUID id, UpdateGalleryImageRequest req) {
        GalleryImage image = getEntity(id);
        image.setImageUrl(req.imageUrl().trim());
        image.setCaption(normalizeCaption(req.caption()));
        image.setAltText(normalizeCaption(req.altText()));
        // null keeps the stored order/flag so an old client can't silently reorder or unpublish.
        if (req.displayOrder() != null) {
            image.setDisplayOrder(req.displayOrder());
        }
        if (req.active() != null) {
            image.setActive(req.active());
        }
        return toDto(galleryImageRepository.save(image));
    }

    public void delete(UUID id) {
        if (!galleryImageRepository.existsById(id)) {
            throw new NotFoundException("Gallery image not found: " + id);
        }
        galleryImageRepository.deleteById(id);
    }

    /** Blank input is stored as null (the public page falls back to its own note). */
    private String normalizeCaption(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private int nextOrder() {
        List<GalleryImage> all = galleryImageRepository.findAllByOrderByDisplayOrderAsc();
        return all.isEmpty() ? 0 : all.get(all.size() - 1).getDisplayOrder() + 1;
    }

    private GalleryImage getEntity(UUID id) {
        return galleryImageRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Gallery image not found: " + id));
    }

    private GalleryImageDto toDto(GalleryImage image) {
        return new GalleryImageDto(
                image.getId(),
                image.getImageUrl(),
                image.getCaption(),
                image.getAltText(),
                image.getDisplayOrder(),
                image.isActive());
    }
}
