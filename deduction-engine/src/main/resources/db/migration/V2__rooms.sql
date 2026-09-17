CREATE TABLE IF NOT EXISTS ${schema}.rooms (
    room_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    host_pseudo VARCHAR(24) NOT NULL,
    guest_pseudo VARCHAR(24),
    access_salt VARCHAR(64),
    access_hash VARCHAR(128),
    host_token_hash VARCHAR(128) NOT NULL,
    guest_token_hash VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS rooms_waiting_created_idx
    ON ${schema}.rooms (created_at DESC)
    WHERE guest_pseudo IS NULL;

INSERT INTO ${schema}.schema_version (version, description)
VALUES (2, 'Rooms with optional access code and player tokens')
ON CONFLICT (version) DO NOTHING;