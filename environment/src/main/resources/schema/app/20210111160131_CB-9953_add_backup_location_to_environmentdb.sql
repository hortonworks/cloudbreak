-- // CB-9953 add backup location to environmentdb
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS backup TEXT DEFAULT NULL;
-- UPDATE environment SET environment_service_version = 'No Info' WHERE environment_service_version IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS backup;
