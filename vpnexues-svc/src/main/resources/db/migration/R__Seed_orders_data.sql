-- Repeatable migration: DEMO DATA REMOVED.
-- Orders, order items, payments, 10 fake customers and their addresses are NO LONGER seeded.
-- This file now only (1) deletes any previously-seeded demo rows (idempotent, FK-safe order)
-- and (2) keeps seeding the 5 testimonials.
-- Seeded UUID prefixes: users a1000000-%, addresses b1000000-%, orders c1000000-% (VPN-2600xx).

-- ============================================================
-- 1. WIPE PREVIOUSLY SEEDED DEMO ROWS (children first)
-- ============================================================

-- 1a. Payments for seeded orders (or any order belonging to a seeded user)
DELETE FROM payments p
USING orders o
WHERE p.order_id = o.id
  AND (o.id::text LIKE 'c1000000-%' OR o.user_id::text LIKE 'a1000000-%');

-- 1b. Order items for seeded orders
DELETE FROM order_items oi
USING orders o
WHERE oi.order_id = o.id
  AND (o.id::text LIKE 'c1000000-%' OR o.user_id::text LIKE 'a1000000-%');

-- 1c. Seeded orders
DELETE FROM orders
WHERE id::text LIKE 'c1000000-%'
   OR user_id::text LIKE 'a1000000-%';

-- 1d. Carts belonging to seeded users
DELETE FROM carts
WHERE user_id::text LIKE 'a1000000-%';

-- 1e. Addresses belonging to seeded users
DELETE FROM user_addresses
WHERE id::text LIKE 'b1000000-%'
   OR user_id::text LIKE 'a1000000-%';

-- 1f. Seeded demo users
DELETE FROM users
WHERE id::text LIKE 'a1000000-%';

-- ============================================================
-- 2. TESTIMONIALS (kept — real content, idempotent)
-- ============================================================
INSERT INTO testimonials (name, role, location, rating, quote)
SELECT * FROM (VALUES
  ('Rajesh Kumar', 'Farmer', 'India', 5, 'VPNexues connects me directly with urban customers. Fresh produce, fair prices.'),
  ('Michelle Wong', 'Home Cook', 'Singapore', 4, 'The Small Box plan is perfect for my weekly meal prep. Always fresh.'),
  ('Ahmed Khalil', 'Restaurant Manager', 'UAE', 5, 'Bulk orders are always on time and the quality is consistent. Highly recommend.'),
  ('Sarah Mitchell', 'Nutritionist', 'USA', 4, 'I recommend VPNexues to all my clients for organic, farm-fresh produce.'),
  ('Deepa Menon', 'Food Blogger', 'India', 5, 'The Alphonso mangoes are the best I have ever tasted. Incredible quality!')
) AS t(name, role, location, rating, quote)
WHERE NOT EXISTS (SELECT 1 FROM testimonials WHERE name = 'Rajesh Kumar');
