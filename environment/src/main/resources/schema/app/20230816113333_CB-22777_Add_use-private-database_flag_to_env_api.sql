-- // CB-22777 - Add use-private-database flag to env api
-- Migration SQL that makes the change goes here.
ALTER TABLE environment_parameters ADD COLUMN IF NOT EXISTS database_setup text;

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment_parameters DROP COLUMN IF EXISTS database_setup;