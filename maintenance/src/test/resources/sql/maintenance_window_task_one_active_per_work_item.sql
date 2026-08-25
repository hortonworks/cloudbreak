-- H2 cannot express PostgreSQL partial indexes (WHERE status = 'ACTIVE'). Including status in the
-- unique key preserves the idempotency invariant under test: at most one row per tuple per status,
-- so concurrent ACTIVE registrations for the same work item still collide.
CREATE UNIQUE INDEX IF NOT EXISTS maintenance_window_task_one_active_per_work_item
   ON maintenance_window_task (account_id, resource_crn, task_type, work_item_id, status);

-- Hibernate omits version from INSERT (insertable = false); Postgres applies DEFAULT 1 from migration.
-- H2 schema generated from JPA is NOT NULL without a default, so mirror production for integration tests.
ALTER TABLE maintenance_window_task ALTER COLUMN version SET DEFAULT 1;
