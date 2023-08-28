-- // CB-22777 - Remove use-private-database flag from env api
-- Migration SQL that makes the change goes here.
ALTER TABLE environment_parameters DROP COLUMN IF EXISTS database_setup;

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment_parameters ADD COLUMN IF NOT EXISTS database_setup text;
