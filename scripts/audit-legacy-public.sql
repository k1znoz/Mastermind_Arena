-- Read-only inventory before removing the five obsolete Mastermind tables.
-- No game rows, connection strings, passwords or routine bodies are returned.
-- Catalog dependencies cannot prove the absence of external clients or dynamic SQL.
WITH legacy AS (
  SELECT c.oid, c.relname, c.relkind, pg_get_userbyid(c.relowner) AS owner
  FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE n.nspname = 'public'
    AND c.relname IN ('rooms', 'match_state', 'idempotency', 'workflow_event', 'schema_version')
), owned_sequences AS (
  SELECT DISTINCT s.oid, s.relname
  FROM pg_depend d
  JOIN legacy t ON d.refclassid = 'pg_class'::regclass AND d.refobjid = t.oid
  JOIN pg_class s ON d.classid = 'pg_class'::regclass AND d.objid = s.oid
  WHERE s.relkind = 'S' AND d.deptype IN ('a', 'i')
), targets AS (
  SELECT oid FROM legacy UNION SELECT oid FROM owned_sequences
)
SELECT jsonb_pretty(jsonb_build_object(
  'tables', COALESCE((SELECT jsonb_agg(jsonb_build_object(
    'name', relname, 'kind', relkind, 'owner', owner
  ) ORDER BY relname) FROM legacy), '[]'::jsonb),
  'owned_sequences', COALESCE((SELECT jsonb_agg(relname ORDER BY relname)
    FROM owned_sequences), '[]'::jsonb),
  'dependencies', COALESCE((SELECT jsonb_agg(jsonb_build_object(
    'referenced', pg_describe_object(d.refclassid, d.refobjid, d.refobjsubid),
    'dependent', pg_describe_object(d.classid, d.objid, d.objsubid),
    'type', d.deptype
  ) ORDER BY d.refobjid, d.classid, d.objid, d.objsubid)
    FROM pg_depend d
    WHERE d.refclassid = 'pg_class'::regclass AND d.refobjid IN (SELECT oid FROM targets)
  ), '[]'::jsonb),
  'routine_candidates', COALESCE((SELECT jsonb_agg(jsonb_build_object(
    'schema', n.nspname, 'name', p.proname,
    'arguments', pg_get_function_identity_arguments(p.oid)
  ) ORDER BY n.nspname, p.proname, p.oid)
    FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname NOT IN ('pg_catalog', 'information_schema')
      AND p.prosrc ~* '\m(rooms|match_state|idempotency|workflow_event|schema_version)\M'
  ), '[]'::jsonb)
)) AS legacy_public_audit;
