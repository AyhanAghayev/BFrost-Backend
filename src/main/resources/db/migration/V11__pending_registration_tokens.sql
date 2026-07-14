CREATE TABLE IF NOT EXISTS pending_registration_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pending_registration_tokens_token ON pending_registration_tokens(token);
CREATE INDEX IF NOT EXISTS idx_pending_registration_tokens_user  ON pending_registration_tokens(user_id);
