-- New installation / empty database only. Existing games are deliberately not imported.
ALTER TABLE ${schema}.idempotency RENAME COLUMN result_state TO result_payload;
ALTER TABLE ${schema}.idempotency DROP COLUMN result_resolution;
ALTER TABLE ${schema}.idempotency DROP COLUMN result_events;
INSERT INTO ${schema}.schema_version (version, description)
VALUES (5, 'Versioned JSON match and idempotency documents')
ON CONFLICT (version) DO NOTHING;
