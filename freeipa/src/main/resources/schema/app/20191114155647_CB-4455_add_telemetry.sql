-- // CB-4455 Add telemetry to freeipa

ALTER TABLE stack ADD COLUMN IF NOT EXISTS telemetry text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS telemetry;