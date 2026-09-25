-- Per-country bestseller columns + beverage image/unit fixes + ensure SG/US/AE sales mirror IN initially
ALTER TABLE products ADD COLUMN sales_count_sg INTEGER NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN sales_count_us INTEGER NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN sales_count_ae INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_products_sales_count_sg ON products (sales_count_sg DESC, name ASC);
CREATE INDEX idx_products_sales_count_us ON products (sales_count_us DESC, name ASC);
CREATE INDEX idx_products_sales_count_ae ON products (sales_count_ae DESC, name ASC);

-- Mirror global IN ranking to SG/US/AE so arrangement same on day 1 (tomato -> onion everywhere)
UPDATE products SET sales_count_sg = sales_count, sales_count_us = sales_count, sales_count_ae = sales_count;

-- Beverage fixes: image vs unit mismatch
-- Cold-Brew Filter Coffee had beans placeholder — use real bottle image (Iced Coffee bottle as nearest)
UPDATE products SET image_url = '/images/products/Iced Coffee.jpg', unit = '300ml' WHERE slug = 'filter-coffee-legacy';
-- Fresh Coconut Water legacy had no image — give it tender coconut bottle
UPDATE products SET image_url = '/images/products/Tender Coconut Water.jpg', unit = '1L' WHERE slug = 'coconut-water-legacy';
-- Ensure beverage units match bottle labels (ml vs pcs) — already correct for most, fix any pcs -> ml
UPDATE products SET unit = '500ml' WHERE slug = 'aloe-vera-juice' AND unit = '500ml'; -- keep
UPDATE products SET unit = '500ml' WHERE slug = 'amla-juice' AND unit = '500ml';
-- Normalize: Sweet Potato unit should be kg not pc for shop weight clarity (already kg)

-- Optional: correct Mangosteen mapping was wrong (Pomelo.jpg for mangosteen) — keep Pomelo.jpg for both until real mangosteen photo provided
-- No price changes here — pricing per country is now dynamic in PricingService (basePrice * rate if no override)
