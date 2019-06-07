-- // CB-1619 Remove environment related properties from cluster template
-- Migration SQL that makes the change goes here.


ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS subnetmetas TEXT NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_network DROP COLUMN IF EXISTS subnetmetas;

