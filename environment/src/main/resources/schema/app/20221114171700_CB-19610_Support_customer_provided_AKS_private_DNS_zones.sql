-- // CB-19610 Support customer provided AKS private DNS zones
-- Migration SQL that makes the change goes here.
ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS aksprivatednszoneid VARCHAR(255);


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment_network DROP COLUMN IF EXISTS aksprivatednszoneid;

