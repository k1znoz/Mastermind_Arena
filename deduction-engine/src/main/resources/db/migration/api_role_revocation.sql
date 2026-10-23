-- Applied only for the fixed roles anon/authenticated when present.
REVOKE ALL ON TABLE ${schema}.rooms, ${schema}.match_state, ${schema}.idempotency,
    ${schema}.workflow_event, ${schema}.schema_version FROM ${role};
ALTER DEFAULT PRIVILEGES IN SCHEMA ${schema} REVOKE ALL ON TABLES FROM ${role};
