-- // CB-33852 persist dispatcher skip reason on maintenance_window_run
-- Maintenance windows are not yet in production; maintenance_window_run is empty in shipped environments.

ALTER TABLE maintenance_window_run
    ADD COLUMN IF NOT EXISTS skip_reason VARCHAR(64);

-- //@UNDO

ALTER TABLE maintenance_window_run
    DROP COLUMN IF EXISTS skip_reason;
