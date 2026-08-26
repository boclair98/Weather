CREATE TABLE IF NOT EXISTS email_verification_challenges (
    id BIGSERIAL PRIMARY KEY,
    owner_id VARCHAR(64) NOT NULL,
    email VARCHAR(254) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    consumed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_email_verification_owner
    ON email_verification_challenges(owner_id);

CREATE INDEX IF NOT EXISTS idx_email_verification_expires
    ON email_verification_challenges(expires_at);
