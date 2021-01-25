-- // CB-10213: Create EFS file system
-- Migration SQL that makes the change goes here.
ALTER TABLE cluster ADD COLUMN IF NOT EXISTS additionalfilesystem_id bigint;

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE cluster DROP COLUMN IF EXISTS additionalfilesystem_id;


