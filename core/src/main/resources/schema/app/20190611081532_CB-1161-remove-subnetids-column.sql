-- // CB-1161-remove-subnetids-column
-- Migration SQL that makes the change goes here.
ALTER TABLE environment_network DROP COLUMN IF EXISTS subnetids;

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS subnetids TEXT NOT NULL;

