-- Admin-configurable Small Box / Big Box pricing (previously hardcoded in PricingService).
-- DEFAULT values below match the previous hardcoded constants exactly, so behavior is
-- unchanged for the existing store_settings row (and any future one) until an admin edits them.

ALTER TABLE store_settings ADD COLUMN small_box_standard_slots INT NOT NULL DEFAULT 10;
ALTER TABLE store_settings ADD COLUMN small_box_premium_rate NUMERIC(6, 4) NOT NULL DEFAULT 0.15;
ALTER TABLE store_settings ADD COLUMN big_box_min_kg NUMERIC(6, 2) NOT NULL DEFAULT 10;
ALTER TABLE store_settings ADD COLUMN big_box_discount_rate NUMERIC(6, 4) NOT NULL DEFAULT 0.12;
