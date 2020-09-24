-- // DISTX-526 crn for alerts
-- Migration SQL that makes the change goes here.

ALTER TABLE timealert ADD COLUMN IF NOT EXISTS alert_crn VARCHAR(255);

ALTER TABLE loadalert ADD COLUMN IF NOT EXISTS alert_crn VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE loadalert DROP COLUMN IF EXISTS alert_crn;

ALTER TABLE timealert DROP COLUMN IF EXISTS alert_crn;
