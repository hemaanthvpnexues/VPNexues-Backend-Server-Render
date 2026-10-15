-- Shop page (IN market) tomato price corrections, per review 14-Oct-2026.
-- The product_price_overrides rows were wrong — Vine Tomatoes even had the
-- discount inverted (struck 20.00 / selling 35.00):
--   Vine Tomatoes   GPNX-002: 35.00 / struck 20.00  ->  30.00 / struck 40.00
--   Cherry Tomatoes GPNX-010: 45.00 / struck 30.00  ->  39.00 / struck 50.00
-- products.discount_pct feeds the "% OFF" badge, so it is recomputed from the
-- corrected prices: (40-30)/40 = 25, (50-39)/50 = 22.
-- Idempotent: re-running writes the same values.

UPDATE product_price_overrides o
SET price = 30.00, old_price = 40.00
FROM products p
WHERE o.product_id = p.id
  AND p.slug = 'vine-tomatoes'
  AND o.country_code = 'IN';

UPDATE product_price_overrides o
SET price = 39.00, old_price = 50.00
FROM products p
WHERE o.product_id = p.id
  AND p.slug = 'cherry-tomatoes'
  AND o.country_code = 'IN';

UPDATE products SET discount_pct = 25 WHERE slug = 'vine-tomatoes';
UPDATE products SET discount_pct = 22 WHERE slug = 'cherry-tomatoes';
