CREATE TABLE users (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), username VARCHAR(50) UNIQUE NOT NULL, email VARCHAR(255) UNIQUE NOT NULL, password_hash VARCHAR(255) NOT NULL, email_verified BOOLEAN NOT NULL DEFAULT FALSE, display_name VARCHAR(100) NOT NULL, bio TEXT, profile_picture_url TEXT, background_url TEXT, university VARCHAR(255), department VARCHAR(255), is_verified BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP NOT NULL DEFAULT NOW(), last_login_at TIMESTAMP, search_vector TSVECTOR GENERATED ALWAYS AS (to_tsvector('english', coalesce(username, '') || ' ' || coalesce(display_name, '') || ' ' || coalesce(bio, ''))) STORED);
CREATE INDEX idx_users_search ON users USING GIN(search_vector);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

CREATE TABLE refresh_tokens (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, token VARCHAR(500) UNIQUE NOT NULL, expires_at TIMESTAMP NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW());
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

CREATE TABLE follows (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), follower_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, followee_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, created_at TIMESTAMP NOT NULL DEFAULT NOW(), UNIQUE(follower_id, followee_id), CHECK(follower_id != followee_id));
CREATE INDEX idx_follows_follower ON follows(follower_id);
CREATE INDEX idx_follows_followee ON follows(followee_id);

CREATE TABLE clubs (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), name VARCHAR(100) NOT NULL, slug VARCHAR(100) UNIQUE NOT NULL, description TEXT, owner_id UUID NOT NULL REFERENCES users(id), is_public BOOLEAN NOT NULL DEFAULT TRUE, category VARCHAR(50) NOT NULL, cover_image_url TEXT, logo_url TEXT, created_at TIMESTAMP NOT NULL DEFAULT NOW(), search_vector TSVECTOR GENERATED ALWAYS AS (to_tsvector('english', coalesce(name, '') || ' ' || coalesce(description, ''))) STORED);
CREATE INDEX idx_clubs_search ON clubs USING GIN(search_vector);
CREATE INDEX idx_clubs_slug ON clubs(slug);
CREATE INDEX idx_clubs_owner ON clubs(owner_id);

CREATE TABLE club_tags (club_id UUID NOT NULL REFERENCES clubs(id) ON DELETE CASCADE, tag VARCHAR(50) NOT NULL, PRIMARY KEY(club_id, tag));

CREATE TABLE memberships (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), club_id UUID NOT NULL REFERENCES clubs(id) ON DELETE CASCADE, user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, role VARCHAR(20) NOT NULL DEFAULT 'MEMBER', joined_at TIMESTAMP NOT NULL DEFAULT NOW(), UNIQUE(club_id, user_id));
CREATE INDEX idx_memberships_club ON memberships(club_id);
CREATE INDEX idx_memberships_user ON memberships(user_id);

CREATE TABLE membership_requests (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), club_id UUID NOT NULL REFERENCES clubs(id) ON DELETE CASCADE, user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, status VARCHAR(20) NOT NULL DEFAULT 'PENDING', created_at TIMESTAMP NOT NULL DEFAULT NOW(), UNIQUE(club_id, user_id));
CREATE INDEX idx_membership_requests_club ON membership_requests(club_id);

CREATE TABLE posts (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, target_type VARCHAR(20) NOT NULL, target_id UUID NOT NULL, post_type VARCHAR(20) NOT NULL DEFAULT 'TEXT', title VARCHAR(255), body TEXT NOT NULL, media_url TEXT, link_url TEXT, channel VARCHAR(100), like_count INT NOT NULL DEFAULT 0, dislike_count INT NOT NULL DEFAULT 0, comment_count INT NOT NULL DEFAULT 0, created_at TIMESTAMP NOT NULL DEFAULT NOW(), search_vector TSVECTOR GENERATED ALWAYS AS (to_tsvector('english', coalesce(title, '') || ' ' || coalesce(body, ''))) STORED);
CREATE INDEX idx_posts_search ON posts USING GIN(search_vector);
CREATE INDEX idx_posts_author ON posts(author_id);
CREATE INDEX idx_posts_target ON posts(target_type, target_id);
CREATE INDEX idx_posts_created ON posts(created_at DESC, id DESC);

CREATE TABLE poll_options (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE, option_text VARCHAR(255) NOT NULL, vote_count INT NOT NULL DEFAULT 0, position INT NOT NULL DEFAULT 0);
CREATE INDEX idx_poll_options_post ON poll_options(post_id);

CREATE TABLE poll_votes (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), option_id UUID NOT NULL REFERENCES poll_options(id) ON DELETE CASCADE, user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, created_at TIMESTAMP NOT NULL DEFAULT NOW(), UNIQUE(option_id, user_id));
CREATE INDEX idx_poll_votes_user ON poll_votes(user_id, option_id);

CREATE TABLE comments (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE, author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, body TEXT NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW());
CREATE INDEX idx_comments_post ON comments(post_id);
CREATE INDEX idx_comments_created ON comments(created_at DESC, id DESC);

CREATE TABLE reactions (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE, user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, reaction_type VARCHAR(10) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW(), UNIQUE(post_id, user_id));
CREATE INDEX idx_reactions_post ON reactions(post_id);
CREATE INDEX idx_reactions_user ON reactions(user_id);

CREATE TABLE events (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), club_id UUID NOT NULL REFERENCES clubs(id) ON DELETE CASCADE, title VARCHAR(255) NOT NULL, description TEXT, cover_image_url TEXT, format VARCHAR(20) NOT NULL DEFAULT 'IN_PERSON', location TEXT, start_time TIMESTAMP NOT NULL, end_time TIMESTAMP, max_members INT, created_by UUID NOT NULL REFERENCES users(id), created_at TIMESTAMP NOT NULL DEFAULT NOW());
CREATE INDEX idx_events_club ON events(club_id);
CREATE INDEX idx_events_start ON events(start_time);

CREATE TABLE rsvps (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE, user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, status VARCHAR(20) NOT NULL DEFAULT 'ATTENDING', responded_at TIMESTAMP NOT NULL DEFAULT NOW(), UNIQUE(event_id, user_id));
CREATE INDEX idx_rsvps_event ON rsvps(event_id);
CREATE INDEX idx_rsvps_user ON rsvps(user_id);

CREATE TABLE conversations (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), user_a_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, user_b_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, created_at TIMESTAMP NOT NULL DEFAULT NOW(), UNIQUE(user_a_id, user_b_id), CHECK(user_a_id < user_b_id));
CREATE INDEX idx_conversations_user_a ON conversations(user_a_id);
CREATE INDEX idx_conversations_user_b ON conversations(user_b_id);

CREATE TABLE messages (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE, sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, body TEXT NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW());
CREATE INDEX idx_messages_conversation ON messages(conversation_id);
CREATE INDEX idx_messages_created ON messages(created_at DESC, id DESC);

CREATE TABLE conversation_clearances (user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE, cleared_at TIMESTAMP NOT NULL DEFAULT NOW(), PRIMARY KEY(user_id, conversation_id));

CREATE TABLE notifications (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, actor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, notification_type VARCHAR(30) NOT NULL, target_id UUID, target_type VARCHAR(30), message TEXT, is_read BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP NOT NULL DEFAULT NOW());
CREATE INDEX idx_notifications_recipient ON notifications(recipient_id);
CREATE INDEX idx_notifications_created ON notifications(created_at DESC, id DESC);

CREATE TABLE saved_posts (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE, created_at TIMESTAMP NOT NULL DEFAULT NOW(), UNIQUE(user_id, post_id));
CREATE INDEX idx_saved_posts_user ON saved_posts(user_id, created_at DESC);
CREATE INDEX idx_saved_posts_post ON saved_posts(post_id);

ALTER TABLE users ADD COLUMN notify_follow BOOLEAN NOT NULL DEFAULT TRUE, ADD COLUMN notify_like BOOLEAN NOT NULL DEFAULT TRUE, ADD COLUMN notify_comment BOOLEAN NOT NULL DEFAULT TRUE, ADD COLUMN notify_join_request BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE clubs
DROP CONSTRAINT clubs_owner_id_fkey,
  ADD CONSTRAINT clubs_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE events
DROP CONSTRAINT events_created_by_fkey,
  ADD CONSTRAINT events_created_by_fkey FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE;

CREATE TABLE conversation_reads (user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE, last_read_at TIMESTAMP NOT NULL DEFAULT NOW(), PRIMARY KEY (user_id, conversation_id));
CREATE INDEX idx_conversation_reads_user ON conversation_reads(user_id);

ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL,
  ADD COLUMN google_id VARCHAR(255) UNIQUE;
CREATE INDEX idx_users_google_id ON users(google_id);

ALTER TABLE users ADD COLUMN registration_status VARCHAR(20) DEFAULT 'COMPLETE';
UPDATE users SET registration_status = 'COMPLETE' WHERE registration_status IS NULL;
ALTER TABLE users ALTER COLUMN registration_status SET NOT NULL;

CREATE TABLE pending_registration_tokens (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, token VARCHAR(255) UNIQUE NOT NULL, expires_at TIMESTAMP NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW());
CREATE INDEX idx_pending_registration_tokens_token ON pending_registration_tokens(token);
CREATE INDEX idx_pending_registration_tokens_user ON pending_registration_tokens(user_id);