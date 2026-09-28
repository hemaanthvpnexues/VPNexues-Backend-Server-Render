-- V30: V29 created support_chat_messages without the updated_at column that
-- BaseEntity requires (ddl-auto: validate). Additive fix, idempotent.
ALTER TABLE support_chat_messages ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
