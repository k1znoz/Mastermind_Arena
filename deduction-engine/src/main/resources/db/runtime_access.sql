-- Explicit owner operation, not a startup migration. All identifiers use PostgreSQL %I quoting.
DO $runtime_access$
DECLARE
    runtime_role text := current_setting('arena.runtime_role');
    game_schema text := current_setting('arena.runtime_schema');
    table_name text;
    operation text;
    rights text;
    condition text;
BEGIN
    EXECUTE format('GRANT USAGE ON SCHEMA %I TO %I', game_schema, runtime_role);
    FOREACH table_name IN ARRAY ARRAY['rooms', 'match_state', 'idempotency', 'workflow_event', 'schema_version'] LOOP
        rights := CASE WHEN table_name = 'schema_version' THEN 'SELECT'
                       WHEN table_name IN ('rooms', 'match_state') THEN 'SELECT, INSERT, UPDATE'
                       ELSE 'SELECT, INSERT' END;
        EXECUTE format('GRANT %s ON %I.%I TO %I', rights, game_schema, table_name, runtime_role);
        FOREACH operation IN ARRAY ARRAY['SELECT', 'INSERT', 'UPDATE'] LOOP
            IF operation = 'INSERT' AND table_name = 'schema_version'
                OR operation = 'UPDATE' AND table_name NOT IN ('rooms', 'match_state') THEN CONTINUE; END IF;
            condition := CASE WHEN operation = 'INSERT' THEN format('WITH CHECK (current_user = %L)', runtime_role)
                              WHEN operation = 'UPDATE' THEN format('USING (current_user = %L) WITH CHECK (current_user = %L)', runtime_role, runtime_role)
                              ELSE format('USING (current_user = %L)', runtime_role) END;
            EXECUTE format('DROP POLICY IF EXISTS %I ON %I.%I', runtime_role || '_' || lower(operation), game_schema, table_name);
            EXECUTE format('CREATE POLICY %I ON %I.%I FOR %s TO %I %s', runtime_role || '_' || lower(operation),
                           game_schema, table_name, operation, runtime_role, condition);
        END LOOP;
    END LOOP;
    EXECUTE format('GRANT USAGE ON SEQUENCE %I.workflow_event_event_id_seq TO %I', game_schema, runtime_role);
END
$runtime_access$;
