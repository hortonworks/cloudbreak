-- // CB-CB-5339 Add backup to freeipa

ALTER TABLE stack ADD COLUMN IF NOT EXISTS backup text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS backup;