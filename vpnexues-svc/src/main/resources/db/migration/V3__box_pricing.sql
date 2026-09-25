-- Small Box / Big Box pricing support.
-- weight_kg: Big Box lines are priced per kg (0.5 kg increments); null for regular/Small Box items.
-- box_discount: aggregate Big Box 12%-past-10kg discount, kept separate from the existing coupon `discount`
-- column so each order clearly shows why its total is what it is (same pattern as delivery_fee/tax/tip).

ALTER TABLE cart_items ADD COLUMN weight_kg NUMERIC(6, 2);
ALTER TABLE order_items ADD COLUMN weight_kg NUMERIC(6, 2);
ALTER TABLE orders ADD COLUMN box_discount NUMERIC(12, 2) NOT NULL DEFAULT 0;
