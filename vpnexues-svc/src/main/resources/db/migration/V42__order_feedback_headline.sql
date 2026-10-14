-- V42: headline for post-order feedback (short review title shown next to the rating).
-- Additive, idempotent (IF NOT EXISTS): safe to re-run on already-migrated DBs.

ALTER TABLE order_feedback ADD COLUMN IF NOT EXISTS headline VARCHAR(120);
