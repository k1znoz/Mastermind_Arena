CREATE SCHEMA IF NOT EXISTS ${schema};
CREATE TABLE IF NOT EXISTS ${schema}.schema_version (
    version INTEGER PRIMARY KEY,
    description TEXT NOT NULL,
    applied_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
