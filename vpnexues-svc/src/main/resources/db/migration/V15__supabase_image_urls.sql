-- V15: Migrate local image paths to Supabase storage URLs.
-- V7/V9/V11 set /images/products/... paths; R__ now uses Supabase URLs for fresh DBs.
-- This migration overrides the local paths for existing databases.
-- Idempotent: only updates rows that still have /images/ paths.

UPDATE products
SET image_url = REPLACE(
        image_url,
        '/images/products/',
        'https://ccecckljdfkyzalsnfig.supabase.co/storage/v1/object/public/product-images/'
    )
WHERE image_url LIKE '%/images/products/%';

UPDATE testimonials
SET photo_url = REPLACE(
        photo_url,
        '/images/testimonial/',
        'https://ccecckljdfkyzalsnfig.supabase.co/storage/v1/object/public/site-assets/testimonial/'
    )
WHERE photo_url LIKE '%/images/testimonial/%';
