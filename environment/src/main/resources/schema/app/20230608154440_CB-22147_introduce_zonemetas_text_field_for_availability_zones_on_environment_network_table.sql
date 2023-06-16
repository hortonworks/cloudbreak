-- // CB-22147 introduce zonemetas text field for availability zones on environment_network table
-- Migration SQL that makes the change goes here.

ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS zonemetas VARCHAR DEFAULT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_network DROP COLUMN IF EXISTS zonemetas;
