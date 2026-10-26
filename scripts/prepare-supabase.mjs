import { mkdir, readFile, writeFile } from 'node:fs/promises'

// Prepare the existing migrations for the Supabase SQL Editor. Never connects to a database.
const schema = 'arena_v1'
const directory = new URL('../deduction-engine/src/main/resources/db/migration/', import.meta.url)
const migrations = ['V1__submit_action_init.sql', 'V2__rooms.sql', 'V3__repair_rooms.sql',
  'V4__private_game_tables.sql', 'V5__json_idempotency.sql', 'V6__match_retention.sql',
  'V7__compact_receipts.sql', 'V8__shared_retention.sql']
let sql = `-- Initial installation only. Execute as the schema owner in Supabase SQL Editor.
-- Refuses an existing arena_v1 schema. Does not modify public, auth or storage.
BEGIN;
SELECT pg_advisory_xact_lock(hashtext('mastermind_arena_schema_migration'));
DO $bootstrap$
BEGIN
  IF to_regnamespace('${schema}') IS NOT NULL THEN
    RAISE EXCEPTION 'ARENA_SCHEMA_ALREADY_EXISTS: inspect arena_v1 before installing';
  END IF;
END;
$bootstrap$;
`
for (const migration of migrations) {
  sql += `\n-- ${migration}\n` + (await readFile(new URL(migration, directory), 'utf8')).replaceAll('${schema}', schema) + '\n'
  if (migration.startsWith('V4')) {
    const revoke = await readFile(new URL('api_role_revocation.sql', directory), 'utf8')
    for (const role of ['anon', 'authenticated']) {
      sql += `DO $revoke$ BEGIN IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${role}') THEN\n`
        + revoke.replaceAll('${schema}', schema).replaceAll('${role}', role)
        + '\nEND IF; END; $revoke$;\n'
    }
  }
}
sql += `\nCOMMIT;\nSELECT current_user AS schema_owner;\nSELECT version, description FROM ${schema}.schema_version ORDER BY version;\n`
const output = new URL('../target/supabase-arena_v1.sql', import.meta.url)
await mkdir(new URL('../target/', import.meta.url), { recursive: true })
await writeFile(output, sql, 'utf8')
console.log('Prepared target/supabase-arena_v1.sql from migrations V1-V8. No database accessed.')
