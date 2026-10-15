-- Fix swapped gourd product images (shop page showed green chillies for Bottle Gourd).
--
-- Root cause: the assets were uploaded to Cloudinary/Supabase under the wrong names.
--   v1790000999/vpnexues/Bottle Gourd.jpg  -> actually a basket of GREEN CHILLIES
--   v1790000995/vpnexues/Bitter Gourd.jpg  -> actually the real BOTTLE GOURD photo
-- So Bitter Gourd (GPNX-028) was showing bottle gourds and Bottle Gourd (GPNX-032)
-- was showing chillies. Point each slug at the asset that really depicts it.
--
-- Guarded with the same cloudinary/supabase condition as V20 so any admin-uploaded
-- /uploads/products/... image is preserved. Idempotent (re-running writes the same value).

UPDATE products SET image_url = 'https://res.cloudinary.com/eqjshlur/image/upload/v1790000995/vpnexues/Bitter%20Gourd.jpg'
WHERE slug = 'bottle-gourd'
  AND (image_url LIKE '%supabase%' OR image_url LIKE '%cloudinary%');

UPDATE products SET image_url = 'https://res.cloudinary.com/eqjshlur/image/upload/v1790000995/vpnexues/Bitter%20Gourd%20Organic.jpg'
WHERE slug = 'bitter-gourd'
  AND (image_url LIKE '%supabase%' OR image_url LIKE '%cloudinary%');
