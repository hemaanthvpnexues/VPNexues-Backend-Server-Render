-- V14: Fix missing ON DELETE actions, add performance indexes, re-apply sales counts
-- These fixes address bugs found in the QA audit (2026-09-12).

-- ── 1. Add missing foreign key ON DELETE actions ──

-- cart_items.product_id: SET NULL if product is deleted (preserve cart)
ALTER TABLE cart_items DROP CONSTRAINT IF EXISTS fk_cart_items_product;
ALTER TABLE cart_items ADD CONSTRAINT fk_cart_items_product
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL;

-- orders.user_id: RESTRICT deleting a user with orders
ALTER TABLE orders DROP CONSTRAINT IF EXISTS fk_orders_user;
ALTER TABLE orders ADD CONSTRAINT fk_orders_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;

-- orders.address_id: RESTRICT deleting an address used by orders
ALTER TABLE orders DROP CONSTRAINT IF EXISTS fk_orders_address;
ALTER TABLE orders ADD CONSTRAINT fk_orders_address
    FOREIGN KEY (address_id) REFERENCES user_addresses(id) ON DELETE RESTRICT;

-- order_items.product_id: SET NULL if product is deleted (preserve order history)
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS fk_order_items_product;
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_product
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL;

-- payments.order_id: CASCADE delete payments when order is deleted
ALTER TABLE payments DROP CONSTRAINT IF EXISTS fk_payments_order;
ALTER TABLE payments ADD CONSTRAINT fk_payments_order
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE;

-- ── 2. Add performance indexes ──

CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_active ON products(active);
CREATE INDEX IF NOT EXISTS idx_orders_country_code ON orders(country_code);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_coupons_active ON coupons(active);

-- ── 3. Re-apply sales counts (V8/V9 ran before R__ on fresh DB) ──

UPDATE products SET sales_count = 1000 WHERE slug = 'farm-tomatoes' AND sales_count = 0;
UPDATE products SET sales_count = 950 WHERE slug = 'red-onions' AND sales_count = 0;
UPDATE products SET sales_count = 900 WHERE slug = 'banana-cavendish' AND sales_count = 0;
UPDATE products SET sales_count = 850 WHERE slug = 'green-chillies' AND sales_count = 0;
UPDATE products SET sales_count = 800 WHERE slug = 'coriander' AND sales_count = 0;
UPDATE products SET sales_count = 750 WHERE slug = 'spinach' AND sales_count = 0;
UPDATE products SET sales_count = 700 WHERE slug = 'potato' AND sales_count = 0;
UPDATE products SET sales_count = 650 WHERE slug = 'carrot' AND sales_count = 0;
UPDATE products SET sales_count = 600 WHERE slug = 'beetroot' AND sales_count = 0;
UPDATE products SET sales_count = 550 WHERE slug = 'cucumber' AND sales_count = 0;
UPDATE products SET sales_count = 500 WHERE slug = 'lemon' AND sales_count = 0;
UPDATE products SET sales_count = 450 WHERE slug = 'ginger' AND sales_count = 0;
UPDATE products SET sales_count = 400 WHERE slug = 'garlic' AND sales_count = 0;
UPDATE products SET sales_count = 350 WHERE slug = 'curry-leaves' AND sales_count = 0;
UPDATE products SET sales_count = 300 WHERE slug = 'mint' AND sales_count = 0;
UPDATE products SET sales_count = 250 WHERE slug = 'raw-mango' AND sales_count = 0;
UPDATE products SET sales_count = 200 WHERE slug = 'coconut-fresh' AND sales_count = 0;
UPDATE products SET sales_count = 180 WHERE slug = 'banana-raw' AND sales_count = 0;
UPDATE products SET sales_count = 160 WHERE slug = 'onion-shallots' AND sales_count = 0;
UPDATE products SET sales_count = 140 WHERE slug = 'drumstick' AND sales_count = 0;

-- Fix incorrect slugs from V8 (these were the wrong slugs)
UPDATE products SET sales_count = 170 WHERE slug = 'orange-navel' AND sales_count = 0;
UPDATE products SET sales_count = 150 WHERE slug = 'pomegranate' AND sales_count = 0;

-- Per-country sales counts
UPDATE products SET sales_count_sg = sales_count WHERE sales_count_sg = 0 AND sales_count > 0;
UPDATE products SET sales_count_us = FLOOR(sales_count * 0.1) WHERE sales_count_us = 0 AND sales_count > 0;
UPDATE products SET sales_count_ae = FLOOR(sales_count * 0.15) WHERE sales_count_ae = 0 AND sales_count > 0;

-- ── 4. Fix wrong units for grocery products ──

UPDATE products SET unit = '250g' WHERE slug = 'frozen-vegetables' AND unit = '250ml';
UPDATE products SET unit = '250g' WHERE slug = 'butter' AND unit = '250ml';
UPDATE products SET unit = '250g' WHERE slug = 'farm-fresh-cow-ghee' AND unit = '250ml';
UPDATE products SET unit = '250g' WHERE slug = 'curd' AND unit = '250ml';
UPDATE products SET unit = '250g' WHERE slug = 'cheese' AND unit = '250ml';

-- ── 5. Fix testimonial name (V12/V13 renames lost on fresh DB) ──

UPDATE testimonials SET name = 'Arp Sathish Kumar' WHERE name = 'Ananya Rao';
UPDATE testimonials SET name = 'Arp Sathish Kumar' WHERE name = 'Sathish Kumar';
