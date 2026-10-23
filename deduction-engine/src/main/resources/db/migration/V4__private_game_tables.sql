-- These tables are owned by the JDBC backend, never read directly by browser roles.
ALTER TABLE ${schema}.rooms ENABLE ROW LEVEL SECURITY;
ALTER TABLE ${schema}.match_state ENABLE ROW LEVEL SECURITY;
ALTER TABLE ${schema}.idempotency ENABLE ROW LEVEL SECURITY;
ALTER TABLE ${schema}.workflow_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE ${schema}.schema_version ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON TABLE ${schema}.rooms, ${schema}.match_state, ${schema}.idempotency,
    ${schema}.workflow_event, ${schema}.schema_version FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA ${schema} REVOKE ALL ON TABLES FROM PUBLIC;

INSERT INTO ${schema}.schema_version (version, description)
VALUES (4, 'Private JDBC game tables: RLS and revoked public access')
ON CONFLICT (version) DO NOTHING;
