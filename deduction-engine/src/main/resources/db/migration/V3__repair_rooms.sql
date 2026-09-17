ALTER TABLE ${schema}.rooms
    ADD COLUMN IF NOT EXISTS guest_pseudo VARCHAR(24),
    ADD COLUMN IF NOT EXISTS access_salt VARCHAR(64),
    ADD COLUMN IF NOT EXISTS access_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS guest_token_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();

CREATE INDEX IF NOT EXISTS rooms_waiting_created_idx
    ON ${schema}.rooms (created_at DESC)
    WHERE guest_pseudo IS NULL;

INSERT INTO ${schema}.schema_version (version, description)
VALUES (3, 'Repair columns required to join existing rooms')
ON CONFLICT (version) DO NOTHING;