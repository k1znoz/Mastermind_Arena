-- Administrative entry point shared by the CLI and pg_cron. CALL outside an explicit transaction.
CREATE PROCEDURE ${schema}.purge_expired_matches(
    IN cutoff timestamptz DEFAULT pg_catalog.clock_timestamp() - INTERVAL '168 hours',
    INOUT deleted_matches bigint DEFAULT 0
)
LANGUAGE plpgsql
SECURITY INVOKER
AS $retention$
DECLARE
    candidate text;
    removed bigint;
BEGIN
    IF cutoff IS NULL THEN
        RAISE EXCEPTION 'RETENTION_CUTOFF_REQUIRED' USING ERRCODE = '22004';
    END IF;
    deleted_matches := 0;
    FOR candidate IN
        SELECT match_id FROM ${schema}.match_state WHERE updated_at <= cutoff ORDER BY match_id
    LOOP
        PERFORM pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtext('${schema}:match:' || candidate));
        -- A separate statement after the lock sees actions committed while this candidate waited.
        DELETE FROM ${schema}.match_state WHERE match_id = candidate AND updated_at <= cutoff;
        GET DIAGNOSTICS removed = ROW_COUNT;
        deleted_matches := deleted_matches + removed;
        -- Release this match's lock; later failures must not undo earlier completed deletions.
        COMMIT;
    END LOOP;
END
$retention$;

REVOKE ALL ON PROCEDURE ${schema}.purge_expired_matches(timestamptz, bigint) FROM PUBLIC;

-- Also remove any EXECUTE rights inherited from owner-specific default privileges.
DO $permissions$
DECLARE
    grantee_name text;
BEGIN
    FOR grantee_name IN
        SELECT DISTINCT roles.rolname
        FROM pg_catalog.pg_proc routines
        JOIN pg_catalog.pg_namespace namespaces ON namespaces.oid = routines.pronamespace
        CROSS JOIN LATERAL pg_catalog.aclexplode(routines.proacl) privileges
        JOIN pg_catalog.pg_roles roles ON roles.oid = privileges.grantee
        WHERE namespaces.oid = '${schema}'::pg_catalog.regnamespace AND routines.proname = 'purge_expired_matches'
          AND routines.prokind = 'p' AND privileges.grantee <> routines.proowner
    LOOP
        EXECUTE pg_catalog.format('REVOKE ALL ON PROCEDURE %I.purge_expired_matches(timestamptz, bigint) FROM %I',
                                  '${schema}', grantee_name);
    END LOOP;
END
$permissions$;

INSERT INTO ${schema}.schema_version (version, description)
VALUES (8, 'Owner-only retention procedure shared by CLI and pg_cron');
