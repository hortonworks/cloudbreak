-- // CB-2902 managed public key add default value
-- Migration SQL that makes the change goes here.

ALTER TABLE environment_authentication ALTER COLUMN managedkey SET DEFAULT false;

UPDATE environment_authentication SET managedkey = false WHERE managedkey IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_authentication ALTER COLUMN managedkey DROP DEFAULT;
