-- V29: live-chat transcript store (chatbot "Talk to Agent" escalations).
-- Idempotent (IF NOT EXISTS): safe to re-run on already-migrated DBs.

CREATE TABLE IF NOT EXISTS support_chat_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    visitor_name    VARCHAR(100) NOT NULL,
    visitor_email   VARCHAR(255) NOT NULL,
    country_code    VARCHAR(4),
    subject         VARCHAR(200) NOT NULL DEFAULT 'Live chat escalation',
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at       TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS support_chat_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID NOT NULL REFERENCES support_chat_sessions(id) ON DELETE CASCADE,
    sender          VARCHAR(10) NOT NULL DEFAULT 'VISITOR',
    body            TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_chat_sessions_status ON support_chat_sessions (status);
CREATE INDEX IF NOT EXISTS idx_chat_sessions_email ON support_chat_sessions (visitor_email);
CREATE INDEX IF NOT EXISTS idx_chat_messages_session ON support_chat_messages (session_id);
