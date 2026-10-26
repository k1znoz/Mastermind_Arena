-- Regression check on disposable PostgreSQL only, after prepare-runtime-role output.
-- Requires an administrative connection able to SET ROLE. Test rows are rolled back.
BEGIN;
DO $checks$
DECLARE flags record;
BEGIN
  SELECT * INTO STRICT flags FROM pg_roles WHERE rolname = 'arena_runtime';
  IF flags.rolcanlogin OR flags.rolsuper OR flags.rolcreatedb OR flags.rolcreaterole
    OR flags.rolinherit OR flags.rolreplication OR flags.rolbypassrls THEN
    RAISE EXCEPTION 'Unexpected initial runtime role flags';
  END IF;
  IF EXISTS (SELECT 1 FROM pg_auth_members WHERE member = flags.oid) THEN
    RAISE EXCEPTION 'Runtime role belongs to another role';
  END IF;
END;
$checks$;
SET LOCAL ROLE arena_runtime;
SELECT count(*) = 8 AS versions_readable FROM arena_v1.schema_version;
INSERT INTO arena_v1.match_state(match_id,payload) VALUES ('runtime-role-regression','{}');
INSERT INTO arena_v1.rooms(room_id,name,host_pseudo,host_token_hash)
  VALUES ('runtime-role-regression','Runtime test','Alice test','synthetic-hash');
UPDATE arena_v1.rooms SET guest_pseudo='Bob test' WHERE room_id='runtime-role-regression';
UPDATE arena_v1.match_state SET updated_at=clock_timestamp() WHERE match_id='runtime-role-regression';
INSERT INTO arena_v1.workflow_event(event_payload,match_id) VALUES ('{}','runtime-role-regression');
INSERT INTO arena_v1.idempotency(idempotency_key,fingerprint,result_payload,match_id)
  VALUES ('runtime-role-regression','synthetic','{"formatVersion":2}','runtime-role-regression');
DO $checks$
DECLARE command text;
BEGIN
  IF (SELECT guest_pseudo FROM arena_v1.rooms WHERE room_id='runtime-role-regression') <> 'Bob test'
    OR (SELECT count(*) FROM arena_v1.workflow_event WHERE match_id='runtime-role-regression') <> 1
    OR (SELECT count(*) FROM arena_v1.idempotency WHERE match_id='runtime-role-regression') <> 1 THEN
    RAISE EXCEPTION 'Expected runtime reads/writes missing';
  END IF;
  FOREACH command IN ARRAY ARRAY[
    'DELETE FROM arena_v1.match_state WHERE false',
    'UPDATE arena_v1.idempotency SET fingerprint=fingerprint WHERE false',
    'UPDATE arena_v1.workflow_event SET event_payload=event_payload WHERE false',
    'UPDATE arena_v1.schema_version SET description=description WHERE false',
    'CREATE TABLE arena_v1.runtime_forbidden(id integer)',
    'ALTER TABLE arena_v1.match_state DISABLE ROW LEVEL SECURITY',
    'TRUNCATE arena_v1.idempotency',
    'CALL arena_v1.purge_expired_matches()'
  ] LOOP
    BEGIN
      EXECUTE command;
      RAISE EXCEPTION 'Forbidden runtime operation accepted: %', command;
    EXCEPTION WHEN insufficient_privilege THEN NULL;
    END;
  END LOOP;
END;
$checks$;
ROLLBACK;
SELECT 'PASS: runtime reads/writes, RLS policies and eight forbidden operations' AS result;
