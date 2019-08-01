-- // CB-2814 Adds application version info column

ALTER TABLE stack
  ADD COLUMN IF NOT EXISTS appversion VARCHAR(255);

-- //@UNDO
ALTER TABLE stack DROP COLUMN IF EXISTS appversion;
