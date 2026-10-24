-- Empty installation only: no conversion of disposable previous game data.
ALTER TABLE ${schema}.rooms ADD CONSTRAINT rooms_match_fk
    FOREIGN KEY (room_id) REFERENCES ${schema}.match_state(match_id) ON DELETE CASCADE;
ALTER TABLE ${schema}.workflow_event ADD COLUMN match_id VARCHAR(255) NOT NULL
    REFERENCES ${schema}.match_state(match_id) ON DELETE CASCADE;
ALTER TABLE ${schema}.idempotency ADD COLUMN match_id VARCHAR(255) NOT NULL
    REFERENCES ${schema}.match_state(match_id) ON DELETE CASCADE;
CREATE INDEX workflow_event_match_idx ON ${schema}.workflow_event(match_id);
CREATE INDEX idempotency_match_idx ON ${schema}.idempotency(match_id);
CREATE INDEX match_state_activity_idx ON ${schema}.match_state(updated_at);
INSERT INTO ${schema}.schema_version (version, description)
VALUES (6, 'Match associations and atomic retention cascade');
