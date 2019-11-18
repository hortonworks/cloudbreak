-- // CB-2902 managed public key
-- Migration SQL that makes the change goes here.

ALTER TABLE environment_authentication ADD COLUMN IF NOT EXISTS managedkey BOOLEAN;

UPDATE environment_authentication SET managedkey = false;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_authentication DROP COLUMN IF EXISTS managedkey;
