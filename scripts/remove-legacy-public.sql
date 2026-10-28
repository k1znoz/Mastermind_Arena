-- Approved initial cleanup only: permanently deletes the five OLD public tables.
-- Run as postgres after reviewing audit-legacy-public.sql and validating arena_v1.
-- Never change these schema-qualified names or add CASCADE.
BEGIN;
SET LOCAL lock_timeout = '5s';

DO $guard$
BEGIN
  IF to_regclass('arena_v1.rooms') IS NULL
     OR to_regclass('arena_v1.match_state') IS NULL
     OR to_regclass('arena_v1.idempotency') IS NULL
     OR to_regclass('arena_v1.workflow_event') IS NULL
     OR to_regclass('arena_v1.schema_version') IS NULL THEN
    RAISE EXCEPTION 'ARENA_V1_TABLES_REQUIRED';
  END IF;
  IF (SELECT array_agg(version ORDER BY version) FROM arena_v1.schema_version)
       IS DISTINCT FROM ARRAY[1,2,3,4,5,6,7,8] THEN
    RAISE EXCEPTION 'ARENA_V1_VERSIONS_REQUIRED';
  END IF;
END;
$guard$;

-- RESTRICT refuses unreviewed external dependencies; any failure rolls back the batch.
-- PostgreSQL also removes internal indexes, constraints and the owned event sequence.
DROP TABLE public.rooms, public.match_state, public.idempotency,
           public.workflow_event, public.schema_version RESTRICT;
COMMIT;

SELECT n.nspname AS schema_name, c.relname AS table_name
FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname IN ('public', 'arena_v1') AND c.relkind IN ('r', 'p')
  AND c.relname IN ('rooms', 'match_state', 'idempotency', 'workflow_event', 'schema_version')
ORDER BY n.nspname, c.relname;
