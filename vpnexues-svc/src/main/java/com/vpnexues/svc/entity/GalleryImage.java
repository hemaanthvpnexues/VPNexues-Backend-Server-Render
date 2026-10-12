package com.vpnexues.svc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A public /gallery photo. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "gallery_images")
public class GalleryImage extends BaseEntity {

    @Column(name = "image_url", nullable = false, columnDefinition = "text")
    private String imageUrl;

    /** Short description shown under the photo and in the lightbox. */
    @Column(length = 500)
    private String caption;

    @Column(name = "alt_text")
    private String altText;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /** Inactive photos are hidden from public /gallery. */
    @Column(nullable = false)
    private boolean active = true;
}
