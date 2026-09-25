-- V19: Admin authentication security hardening
-- Adds brute-force protection, refresh tokens, login audit, country-based SUB_ADMIN, token versioning.

-- ─── admin_users: new columns ───────────────────────────────────────────────

ALTER TABLE admin_users
    ADD COLUMN country_code      VARCHAR(2),
    ADD COLUMN token_version     INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- Index for country-based SUB_ADMIN queries
CREATE INDEX idx_admin_users_country_code ON admin_users (country_code);

-- ─── admin_refresh_tokens: server-controlled session model ───────────────────

CREATE TABLE admin_refresh_tokens (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash        VARCHAR(64) NOT NULL,
    admin_id          UUID NOT NULL,
    session_id        UUID NOT NULL,
    expires_at        TIMESTAMPTZ NOT NULL,
    revoked           BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at        TIMESTAMPTZ,
    replaced_by_id    UUID,
    created_ip        VARCHAR(45),
    user_agent        TEXT,

    CONSTRAINT fk_admin_refresh_tokens_admin
        FOREIGN KEY (admin_id) REFERENCES admin_users(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_admin_refresh_tokens_hash ON admin_refresh_tokens (token_hash);
CREATE INDEX idx_admin_refresh_tokens_admin_id ON admin_refresh_tokens (admin_id);
CREATE INDEX idx_admin_refresh_tokens_session_id ON admin_refresh_tokens (session_id);
CREATE INDEX idx_admin_refresh_tokens_expires ON admin_refresh_tokens (expires_at);

-- ─── admin_login_audit: security audit trail ────────────────────────────────

CREATE TABLE admin_login_audit (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id          UUID,
    email_attempted   VARCHAR(255) NOT NULL,
    ip_address        VARCHAR(45),
    user_agent        TEXT,
    success           BOOLEAN NOT NULL,
    failure_reason    VARCHAR(100),
    attempted_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_admin_login_audit_admin
        FOREIGN KEY (admin_id) REFERENCES admin_users(id) ON DELETE SET NULL
);

CREATE INDEX idx_admin_login_audit_admin_id ON admin_login_audit (admin_id);
CREATE INDEX idx_admin_login_audit_attempted_at ON admin_login_audit (attempted_at);
CREATE INDEX idx_admin_login_audit_email ON admin_login_audit (email_attempted);
