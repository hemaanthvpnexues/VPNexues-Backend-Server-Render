-- V23: re-issue of the firebase_uid change (previously V19__add_firebase_uid_to_users,
-- renamed append-only: V19 is taken by V19__admin_auth_security on already-migrated DBs).
-- Idempotent (IF NOT EXISTS); no-op where V21 already applied it.
ALTER TABLE users ADD COLUMN IF NOT EXISTS firebase_uid VARCHAR(255) UNIQUE;

-- Create index for fast lookups by Firebase UID
CREATE INDEX IF NOT EXISTS idx_users_firebase_uid ON users (firebase_uid);
