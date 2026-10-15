-- Shop page (IN market) tomato prices — final values, review 14-Oct-2026.
-- V42 set Cherry Tomatoes to 39.00; the required selling price is 49.00.
-- V42 also only UPDATEd pre-existing override rows (a no-op where the IN row
-- was missing), so this migration upserts both rows instead:
--   Vine Tomatoes   GPNX-002: price 30.00 / struck 40.00
--   Cherry Tomatoes GPNX-010: price 49.00 / struck 50.00
-- products.discount_pct feeds the "% OFF" badge on the shop card and is set to
-- the marketing figures (not derived from the two price pairs): 21 and 23.
-- Idempotent: re-running writes the same values. Skips cleanly on a fresh DB
-- where R__Seed_data.sql has not inserted the products yet (the repeatable
-- seed carries the same prices).

INSERT INTO product_price_overrides (product_id, country_code, price, old_price)
SELECT id, 'IN', 30.00, 40.00
FROM products
WHERE slug = 'vine-tomatoes'
ON CONFLICT (product_id, country_code)
DO UPDATE SET price = 30.00, old_price = 40.00, updated_at = now();

INSERT INTO product_price_overrides (product_id, country_code, price, old_price)
SELECT id, 'IN', 49.00, 50.00
FROM products
WHERE slug = 'cherry-tomatoes'
ON CONFLICT (product_id, country_code)
DO UPDATE SET price = 49.00, old_price = 50.00, updated_at = now();

UPDATE products SET discount_pct = 21 WHERE slug = 'vine-tomatoes';
UPDATE products SET discount_pct = 23 WHERE slug = 'cherry-tomatoes';
