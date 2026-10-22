-- V50: Sub-Admin one-time access pass (Feature B — Admin Dashboard proposal, 19 Oct 2026)
--
-- A Super Admin issues a short-lived, single-use passcode for a delegated admin account.
-- The passcode must be entered together with the password at login; without it no session
-- is issued. Only the SHA-256 hash is stored — the plaintext is displayed exactly once and
-- never persisted, logged or emailed.
--
-- Idempotent (IF NOT EXISTS): house rule for this repo — these files may be re-run on DBs
-- that were merged out-of-order.

CREATE TABLE IF NOT EXISTS admin_access_passes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id        UUID NOT NULL,
    generated_by    UUID,
    passcode_hash   VARCHAR(64) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    attempts        INTEGER NOT NULL DEFAULT 0,
    invalidated     BOOLEAN NOT NULL DEFAULT FALSE,
    invalidated_at  TIMESTAMPTZ,
    used_at         TIMESTAMPTZ,
    used_ip         VARCHAR(45),
    created_ip      VARCHAR(45),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_admin_access_passes_admin
        FOREIGN KEY (admin_id) REFERENCES admin_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_admin_access_passes_generated_by
        FOREIGN KEY (generated_by) REFERENCES admin_users(id) ON DELETE SET NULL
);

-- "Latest pass for this account" — the login gate reads exactly one row.
CREATE INDEX IF NOT EXISTS idx_admin_access_passes_admin_id ON admin_access_passes (admin_id);
-- Audit list on the Sub-Admin detail screen.
CREATE INDEX IF NOT EXISTS idx_admin_access_passes_created_at ON admin_access_passes (created_at);
