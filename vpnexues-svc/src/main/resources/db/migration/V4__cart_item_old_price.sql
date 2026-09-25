-- Snapshot the product's old/MRP price onto the cart item at add-time (mirrors unit_price_snapshot),
-- so the cart can show a per-line "you saved X" without re-resolving pricing at read time.

ALTER TABLE cart_items ADD COLUMN old_price_snapshot NUMERIC(12, 2);
