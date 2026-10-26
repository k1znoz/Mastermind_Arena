import { mkdir, readFile, writeFile } from 'node:fs/promises'

// Owner operation prepared for SQL Editor. No connection, password or login activation.
const access = await readFile(new URL('../deduction-engine/src/main/resources/db/runtime_access.sql', import.meta.url), 'utf8')
const sql = `-- Create the approved role without changing the current backend connection.
-- Execute as the arena_v1 owner. Refuses a role that already exists.
BEGIN;
SELECT pg_advisory_xact_lock(hashtext('mastermind_arena_schema_migration'));
DO $checks$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'arena_runtime') THEN
    RAISE EXCEPTION 'ARENA_RUNTIME_ROLE_ALREADY_EXISTS';
  END IF;
  IF (SELECT array_agg(version ORDER BY version) FROM arena_v1.schema_version)
      IS DISTINCT FROM ARRAY[1,2,3,4,5,6,7,8] THEN
    RAISE EXCEPTION 'SCHEMA_NOT_READY';
  END IF;
END;
$checks$;
CREATE ROLE arena_runtime NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
  NOINHERIT NOREPLICATION NOBYPASSRLS;
SELECT set_config('arena.runtime_role', 'arena_runtime', true);
SELECT set_config('arena.runtime_schema', 'arena_v1', true);
${access}
COMMIT;
SELECT rolname, rolcanlogin, rolsuper, rolcreatedb, rolcreaterole, rolinherit, rolreplication, rolbypassrls
FROM pg_roles WHERE rolname = 'arena_runtime';
`
const output = new URL('../target/supabase-arena-runtime.sql', import.meta.url)
await mkdir(new URL('../target/', import.meta.url), { recursive: true })
await writeFile(output, sql, 'utf8')
const activation = `-- Copy into a private SQL Editor query, then replace only the assigned value.
-- Never put the actual password into this local file or send it in chat.
DO $activate$
DECLARE
  private_password text := 'REMPLACER_EN_PRIVE';
BEGIN
  IF private_password = 'REMPLACER_' || 'EN_PRIVE' OR length(private_password) < 24 THEN
    RAISE EXCEPTION 'PRIVATE_PASSWORD_REQUIRED: use a unique generated password of at least 24 characters';
  END IF;
  EXECUTE format('ALTER ROLE arena_runtime LOGIN PASSWORD %L', private_password);
END;
$activate$;
SELECT rolname, rolcanlogin FROM pg_roles WHERE rolname = 'arena_runtime';
`
await writeFile(new URL('../target/supabase-arena-runtime-password.sql', import.meta.url), activation, 'utf8')
console.log('Prepared target/supabase-arena-runtime.sql. No password or database connection used.')
console.log('Prepared guarded password template target/supabase-arena-runtime-password.sql.')
