-- // CB-34194 Add lowstorage to dbstack
-- Migration SQL that makes the change goes here.

ALTER TABLE dbstack ADD COLUMN IF NOT EXISTS lowstorage BOOLEAN NOT NULL DEFAULT FALSE;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE dbstack DROP COLUMN IF EXISTS lowstorage;
