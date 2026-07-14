ALTER TABLE clubs ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
UPDATE clubs SET status = 'APPROVED';

CREATE INDEX idx_clubs_status ON clubs(status);
