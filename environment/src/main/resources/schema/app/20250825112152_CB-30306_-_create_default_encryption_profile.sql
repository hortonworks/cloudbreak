-- // CB-30306 - Create default encryption profiles
-- Migration SQL that makes the change goes here.

ALTER TABLE environment
    ADD COLUMN IF NOT EXISTS encryption_profile_name varchar(255) NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS encryption_profile_name;