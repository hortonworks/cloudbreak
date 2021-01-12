-- // CB-10241-Data Hub page should show the runtime version instead of the blueprint version
-- Migration SQL that makes the change goes here.
ALTER TABLE stack ADD COLUMN IF NOT EXISTS stackversion VARCHAR (255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS stackversion;


