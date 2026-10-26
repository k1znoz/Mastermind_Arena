-- Read-only metadata report. Does not read game payloads, passwords or tokens.
-- The SQL Editor identity below is not proof of the backend JDBC identity.
WITH game_tables AS (
  SELECT c.oid, n.nspname AS schema_name, c.relname AS table_name,
         pg_get_userbyid(c.relowner) AS owner, c.relrowsecurity AS rls,
         c.relforcerowsecurity AS force_rls, c.relacl, c.relowner
  FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE n.nspname = 'arena_v1' AND c.relkind IN ('r', 'p')
    AND c.relname IN ('rooms', 'match_state', 'idempotency', 'workflow_event', 'schema_version')
), api_roles AS (
  SELECT oid, rolname, rolsuper, rolbypassrls FROM pg_roles
  WHERE rolname IN ('anon', 'authenticated')
), old_tables AS (
  SELECT c.oid, c.relname AS table_name, pg_get_userbyid(c.relowner) AS owner,
         c.relrowsecurity AS rls
  FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p')
    AND c.relname IN ('rooms', 'match_state', 'idempotency', 'workflow_event', 'schema_version')
)
SELECT jsonb_pretty(jsonb_build_object(
  'sql_editor_role', current_user,
  'postgres_version', current_setting('server_version'),
  'arena_schema_exists', to_regnamespace('arena_v1') IS NOT NULL,
  'game_tables', COALESCE((SELECT jsonb_agg(jsonb_build_object(
    'table', table_name, 'owner', owner, 'rls', rls, 'force_rls', force_rls,
    'public_privileges', COALESCE((SELECT jsonb_agg(a.privilege_type ORDER BY a.privilege_type)
      FROM aclexplode(COALESCE(t.relacl, acldefault('r', t.relowner))) a WHERE a.grantee = 0), '[]'::jsonb)
  ) ORDER BY table_name) FROM game_tables t), '[]'::jsonb),
  'api_access', COALESCE((SELECT jsonb_agg(jsonb_build_object(
    'role', r.rolname, 'table', t.table_name,
    'schema_usage', has_schema_privilege(r.oid, 'arena_v1', 'USAGE'),
    'select', has_table_privilege(r.oid, t.oid, 'SELECT'),
    'insert', has_table_privilege(r.oid, t.oid, 'INSERT'),
    'update', has_table_privilege(r.oid, t.oid, 'UPDATE'),
    'delete', has_table_privilege(r.oid, t.oid, 'DELETE'),
    'other_table_privileges', has_table_privilege(r.oid, t.oid, 'TRUNCATE,REFERENCES,TRIGGER'),
    'maintain', CASE WHEN current_setting('server_version_num')::integer >= 170000
      THEN has_table_privilege(r.oid, t.oid, 'MAINTAIN') ELSE false END,
    'superuser', r.rolsuper, 'bypass_rls', r.rolbypassrls
  ) ORDER BY r.rolname, t.table_name) FROM game_tables t CROSS JOIN api_roles r), '[]'::jsonb),
  'table_policies', COALESCE((SELECT jsonb_agg(jsonb_build_object(
    'table', tablename, 'policy', policyname, 'roles', roles, 'command', cmd
  ) ORDER BY tablename, policyname) FROM pg_policies WHERE schemaname = 'arena_v1'), '[]'::jsonb),
  'purge', COALESCE((SELECT jsonb_agg(jsonb_build_object(
    'owner', pg_get_userbyid(p.proowner), 'security_definer', p.prosecdef,
    'arguments', pg_get_function_identity_arguments(p.oid),
    'api_execute', COALESCE((SELECT jsonb_agg(jsonb_build_object(
      'role', r.rolname, 'execute', has_function_privilege(r.oid, p.oid, 'EXECUTE')
    ) ORDER BY r.rolname) FROM api_roles r), '[]'::jsonb)
  )) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'arena_v1' AND p.proname = 'purge_expired_matches' AND p.prokind = 'p'), '[]'::jsonb),
  'database_roles', COALESCE((SELECT jsonb_agg(jsonb_build_object(
    'role', rolname, 'can_login', rolcanlogin, 'superuser', rolsuper,
    'bypass_rls', rolbypassrls, 'create_role', rolcreaterole, 'create_database', rolcreatedb
  ) ORDER BY rolname) FROM pg_roles WHERE rolname IN (
    SELECT owner FROM game_tables UNION SELECT rolname FROM api_roles
  )), '[]'::jsonb),
  'connection_role_hints', COALESCE((SELECT jsonb_agg(jsonb_build_object(
    'role', usename, 'application', application_name, 'connections', connections
  ) ORDER BY usename, application_name) FROM (
    SELECT usename, application_name, count(*) AS connections
    FROM pg_stat_activity WHERE datname = current_database() AND backend_type = 'client backend'
    GROUP BY usename, application_name
  ) sessions), '[]'::jsonb),
  'old_public_tables', COALESCE((SELECT jsonb_agg(jsonb_build_object(
    'table', table_name, 'owner', owner, 'rls', rls
  ) ORDER BY table_name) FROM old_tables), '[]'::jsonb),
  'foreign_keys_involving_old_tables', COALESCE((SELECT jsonb_agg(jsonb_build_object(
    'constraint', conname, 'source', conrelid::regclass::text, 'target', confrelid::regclass::text
  ) ORDER BY conname) FROM pg_constraint WHERE contype = 'f'
    AND (conrelid IN (SELECT oid FROM old_tables) OR confrelid IN (SELECT oid FROM old_tables))), '[]'::jsonb),
  'cron_extension', (SELECT extversion FROM pg_extension WHERE extname = 'pg_cron'),
  'cron_timezone', current_setting('cron.timezone', true)
)) AS mastermind_audit;
