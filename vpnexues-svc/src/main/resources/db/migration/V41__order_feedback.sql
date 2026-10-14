-- V41: post-order customer feedback (rating + improvement tags + comment).
-- Collected from the Order Success page's feedback popup; anonymous-friendly
-- (no user_id FK — guests can rate too; order_number is a soft reference).
-- Idempotent (IF NOT EXISTS): safe to re-run on already-migrated DBs.

CREATE TABLE IF NOT EXISTS order_feedback (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number  VARCHAR(30),
    rating        SMALLINT NOT NULL,
    tags          VARCHAR(60)[],
    comment       TEXT,
    country_code  VARCHAR(4),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(30),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_order_feedback_rating CHECK (rating >= 1 AND rating <= 5)
);

CREATE INDEX IF NOT EXISTS idx_order_feedback_created ON order_feedback (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_feedback_rating ON order_feedback (rating);
