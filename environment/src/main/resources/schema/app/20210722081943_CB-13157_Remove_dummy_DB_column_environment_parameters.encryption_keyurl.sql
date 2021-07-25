-- // CB-13157: Remove dummy DB column environment_parameters.encryption_keyurl
-- Migration SQL that makes the change goes here.
ALTER TABLE environment_parameters DROP COLUMN IF EXISTS encryption_keyUrl;


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment_parameters ADD COLUMN IF NOT EXISTS encryption_keyUrl text;

