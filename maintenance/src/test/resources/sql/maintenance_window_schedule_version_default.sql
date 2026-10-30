-- Hibernate omits version from INSERT (insertable = false); Postgres applies DEFAULT 1 from migration.
-- H2 schema generated from JPA is NOT NULL without a default, so mirror production for integration tests.
ALTER TABLE maintenance_window_schedule ALTER COLUMN version SET DEFAULT 1;
