CREATE TABLE conversation_reads (
    user_id         UUID      NOT NULL REFERENCES users(id)         ON DELETE CASCADE,
    conversation_id UUID      NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    last_read_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, conversation_id)
);

CREATE INDEX idx_conversation_reads_user ON conversation_reads(user_id);
