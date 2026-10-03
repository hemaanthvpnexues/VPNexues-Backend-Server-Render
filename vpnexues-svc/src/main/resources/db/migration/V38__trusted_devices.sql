-- V38: trusted-device binding for customer phone login.
--
-- Before this migration, POST /api/auth/login issued a 30-day session for ANY
-- registered phone number — anyone who knew the number could sign in. Now the
-- backend only issues that session when the caller also presents the `vpx_dv`
-- cookie (an opaque 256-bit token) of a device that has already passed OTP.
-- Unknown devices get `requiresOtp: true` instead of a session.
--
-- Only the SHA-256 hash of the token is stored — a database leak does not leak
-- usable device tokens.
--
-- IF NOT EXISTS: old-code checkouts (no V38 file) periodically run Flyway repair()
-- against this shared DB and write type='DELETE' tombstones over applied rows,
-- which makes the next new-code boot re-run this file. Without the guards that
-- re-run crashed with 42P07 "relation user_devices already exists"; now it is
-- a harmless no-op that also re-heals the history.
CREATE TABLE IF NOT EXISTS user_devices (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    device_token_hash VARCHAR(64) NOT NULL,
    user_agent        VARCHAR(512),
    ip                VARCHAR(64),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at        TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_user_devices_user_token
    ON user_devices (user_id, device_token_hash);

CREATE INDEX IF NOT EXISTS idx_user_devices_user_active
    ON user_devices (user_id)
    WHERE revoked_at IS NULL;
