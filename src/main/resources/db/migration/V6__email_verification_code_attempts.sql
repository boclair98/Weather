-- Email verification is now completed with a short-lived six-digit code.
-- Keep the existing token hash column for compatibility with previously issued
-- challenges, and persist failed attempts so a code cannot be guessed forever.
ALTER TABLE email_verification_challenges
    ADD COLUMN IF NOT EXISTS failed_attempts INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_email_verification_owner_email_created
    ON email_verification_challenges(owner_id, email, created_at DESC);
