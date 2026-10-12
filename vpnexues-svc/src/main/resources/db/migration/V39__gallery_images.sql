-- Public /gallery photos, managed from the admin dashboard (same pattern as
-- V34 team/news: seeded from the static site data so /gallery renders
-- identically after switching to API-backed content).
-- ids are deterministic md5 UUIDs: a re-run on another machine derives the same
-- rows without needing ON CONFLICT guards.

CREATE TABLE gallery_images (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    image_url     TEXT NOT NULL,
    caption       VARCHAR(500),
    alt_text      VARCHAR(255),
    display_order INTEGER NOT NULL DEFAULT 0,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_gallery_images_display_order ON gallery_images (display_order);

-- ---------------------------------------------------------------------------
-- Seed: the static site's gallery grid (GALLERY in vpnexues-ui/src/pages/marketing/Gallery.tsx)
-- ---------------------------------------------------------------------------
INSERT INTO gallery_images (id, image_url, display_order)
VALUES
(md5('vpnexues-gallery:1')::uuid,  'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/about/aboutusimg7.jpeg', 0),
(md5('vpnexues-gallery:2')::uuid,  'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/about/farmerimage2.png', 1),
(md5('vpnexues-gallery:3')::uuid,  'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/about/farmerimage5.png', 2),
(md5('vpnexues-gallery:4')::uuid,  'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/about/aboutusimg12.jpeg', 3),
(md5('vpnexues-gallery:5')::uuid,  'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/about/farmerimage6.png', 4),
(md5('vpnexues-gallery:6')::uuid,  'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/about/farmerimage.png', 5),
(md5('vpnexues-gallery:7')::uuid,  'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/gallery/farmerimg1.jpg', 6),
(md5('vpnexues-gallery:8')::uuid,  'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/gallery/farmerimg2.jpg', 7),
(md5('vpnexues-gallery:9')::uuid,  'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/gallery/farmerimg3.jpg', 8),
(md5('vpnexues-gallery:10')::uuid, 'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/gallery/farmerimg4.jpg', 9),
(md5('vpnexues-gallery:11')::uuid, 'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/gallery/farmerimg5.jpg', 10),
(md5('vpnexues-gallery:12')::uuid, 'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/gallery/farmerimg6.jpg', 11),
(md5('vpnexues-gallery:13')::uuid, 'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/gallery/farmerimg7.jpg', 12),
(md5('vpnexues-gallery:14')::uuid, 'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/gallery/farmerimg8.jpg', 13),
(md5('vpnexues-gallery:15')::uuid, 'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/gallery/farmerimg9.jpg', 14),
(md5('vpnexues-gallery:16')::uuid, 'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/gallery/farmerimg10.jpg', 15),
(md5('vpnexues-gallery:17')::uuid, 'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/gallery/farmerimg11.jpg', 16),
(md5('vpnexues-gallery:18')::uuid, 'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/gallery/farmerimg12.jpg', 17),
(md5('vpnexues-gallery:19')::uuid, 'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/gallery/farmerimg13.jpg', 18),
(md5('vpnexues-gallery:20')::uuid, 'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/gallery/farmerimg14.jpg', 19),
(md5('vpnexues-gallery:21')::uuid, 'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/gallery/farmerimg15.jpg', 20),
(md5('vpnexues-gallery:22')::uuid, 'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/gallery/farmerimg16.jpg', 21),
(md5('vpnexues-gallery:23')::uuid, 'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/gallery/farmerimg17.jpg', 22),
(md5('vpnexues-gallery:24')::uuid, 'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/gallery/farmerimg18.jpg', 23),
(md5('vpnexues-gallery:25')::uuid, 'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/gallery/farmerimg20.jpg', 24),
(md5('vpnexues-gallery:26')::uuid, 'https://res.cloudinary.com/eqjshlur/image/upload/f_auto,q_auto:good/vpnexues/gallery/farmerimg22.jpg', 25);
