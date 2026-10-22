-- Career jobs: managed from the sub-admin dashboard (careers tab).
-- Idempotent on purpose: an old build's repair() tombstoned this migration in
-- the shared prod DB, so it is re-run on every boot until it succeeds — every
-- statement must survive "already exists" (20-Oct-2026: bare CREATE INDEX on
-- line 20 crashed local + EC2 boots; IF NOT EXISTS added).
CREATE TABLE IF NOT EXISTS career_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    location VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    skills TEXT NOT NULL DEFAULT '',
    duration VARCHAR(100),
    stipend VARCHAR(100),
    start_date VARCHAR(100),
    about TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_career_jobs_active ON career_jobs (active);
CREATE INDEX IF NOT EXISTS idx_career_jobs_display_order ON career_jobs (display_order);
