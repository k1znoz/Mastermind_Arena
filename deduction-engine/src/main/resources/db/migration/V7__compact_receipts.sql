-- Initial empty installation: old full results are deliberately not converted.
ALTER TABLE ${schema}.idempotency ADD CONSTRAINT idempotency_receipt_format
    CHECK (result_payload::jsonb @> '{"formatVersion":2}'::jsonb);
INSERT INTO ${schema}.schema_version (version, description)
VALUES (7, 'Compact action receipts without historical snapshots');
