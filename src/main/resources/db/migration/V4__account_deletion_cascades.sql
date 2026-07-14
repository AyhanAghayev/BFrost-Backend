ALTER TABLE clubs DROP CONSTRAINT clubs_owner_id_fkey;
ALTER TABLE clubs ADD CONSTRAINT clubs_owner_id_fkey
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE events DROP CONSTRAINT events_created_by_fkey;
ALTER TABLE events ADD CONSTRAINT events_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE;
