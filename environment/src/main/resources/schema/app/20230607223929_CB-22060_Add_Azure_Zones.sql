-- // CB-22060 Add Azure Network Attributes
-- Migration SQL that makes the change goes here.
ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS zonemetas text;



-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment_network DROP COLUMN IF EXISTS zonemetas;


