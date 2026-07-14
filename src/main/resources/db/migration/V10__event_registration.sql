ALTER TABLE rsvps ADD COLUMN attended BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE event_questions (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id  UUID         NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    label     VARCHAR(255) NOT NULL,
    type      VARCHAR(20)  NOT NULL,  
    required  BOOLEAN      NOT NULL DEFAULT FALSE,
    position  INT          NOT NULL DEFAULT 0
);
CREATE INDEX idx_event_questions_event ON event_questions(event_id, position);

CREATE TABLE event_question_options (
    question_id UUID         NOT NULL REFERENCES event_questions(id) ON DELETE CASCADE,
    position    INT          NOT NULL,
    opt         VARCHAR(255) NOT NULL,
    PRIMARY KEY (question_id, position)
);
CREATE TABLE rsvp_answers (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rsvp_id     UUID NOT NULL REFERENCES rsvps(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES event_questions(id) ON DELETE CASCADE,
    value       TEXT
);
CREATE INDEX idx_rsvp_answers_rsvp ON rsvp_answers(rsvp_id);
