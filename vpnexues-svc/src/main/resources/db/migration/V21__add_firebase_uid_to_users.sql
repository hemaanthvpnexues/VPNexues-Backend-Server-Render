-- V21: Add firebase_uid column (V19 was lost during Flyway repair on Supabase DB)
ALTER TABLE users ADD COLUMN IF NOT EXISTS firebase_uid VARCHAR(255) UNIQUE;
CREATE INDEX IF NOT EXISTS idx_users_firebase_uid ON users (firebase_uid);
