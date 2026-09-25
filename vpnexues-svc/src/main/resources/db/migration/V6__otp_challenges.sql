-- Shared, persistent OTP challenge store — replaces the in-memory map StubOtpProvider used to
-- keep on its own, so all OTP channels (phone-login via WhatsApp, email verification, and the
-- dev stub provider) share one code path and survive app restarts / work across instances.
CREATE TABLE otp_challenges (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel             VARCHAR(20) NOT NULL,   -- 'PHONE' or 'EMAIL'
    identifier          VARCHAR(255) NOT NULL,  -- phone number or email address
    code                VARCHAR(10) NOT NULL,
    expires_at          TIMESTAMPTZ NOT NULL,
    attempts_remaining  INT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_otp_channel_identifier UNIQUE (channel, identifier)
);

-- Email verification is a post-registration profile action (see EmailVerificationService) —
-- users.email may be set without being verified; email_verified tracks whether the last-saved
-- address was actually confirmed via a code sent to it.
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
