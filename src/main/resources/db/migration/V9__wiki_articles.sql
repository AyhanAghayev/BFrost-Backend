CREATE TABLE wiki_articles (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    club_id        UUID         NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    author_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    last_editor_id UUID         REFERENCES users(id) ON DELETE SET NULL,
    title          VARCHAR(255) NOT NULL,
    summary        VARCHAR(300),
    body           TEXT         NOT NULL,
    is_featured    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wiki_club    ON wiki_articles(club_id, updated_at DESC);
CREATE INDEX idx_wiki_author  ON wiki_articles(author_id);
CREATE UNIQUE INDEX idx_wiki_one_featured ON wiki_articles(club_id) WHERE is_featured;
