-- // CB-33852 track per-occurrence dispatch attempts on maintenance_window_run
-- Maintenance windows are not yet in production; maintenance_window_run is empty in shipped environments,
-- so NOT NULL DEFAULT 1 does not backfill real attempt history.

ALTER TABLE maintenance_window_run
    ADD COLUMN IF NOT EXISTS attempt_count INTEGER NOT NULL DEFAULT 1;

ALTER TABLE maintenance_window_run
    DROP CONSTRAINT IF EXISTS maintenance_window_run_attempt_count_chk;

ALTER TABLE maintenance_window_run
    ADD CONSTRAINT maintenance_window_run_attempt_count_chk
        CHECK (attempt_count >= 1);

-- //@UNDO

ALTER TABLE maintenance_window_run
    DROP CONSTRAINT IF EXISTS maintenance_window_run_attempt_count_chk;

ALTER TABLE maintenance_window_run
    DROP COLUMN IF EXISTS attempt_count;
