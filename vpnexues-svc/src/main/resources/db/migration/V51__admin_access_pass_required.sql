-- V51: scope the Feature B one-time access pass to accounts created AFTER this
-- migration — i.e. "the 5th sub-admin onward".
--
-- The four pre-existing country sub-admins (IN / USA / AED / SG) keep working
-- exactly as they did before Feature B: password alone, no pass. Every account
-- created later through Add Sub Admin requires the Super Admin's one-time pass
-- on every login (AdminAuthController step 2.5).
--
-- Idempotent and additive-only (IF NOT EXISTS + a backfill that no-ops once the
-- rows are FALSE), so it is safe to re-run on any environment, per the V1 rule.

ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS access_pass_required BOOLEAN NOT NULL DEFAULT TRUE;

-- Everything already in admin_users predates the rule, so it stays exempt.
-- DEFAULT TRUE therefore applies only to rows inserted after this migration
-- runs — the new sub-admins. Flyway applies this exactly once, so a freshly
-- created admin can never be swept up by the backfill.
UPDATE admin_users SET access_pass_required = FALSE WHERE access_pass_required;
