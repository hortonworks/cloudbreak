-- // CB-6110 Update DL API with external rds options
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS databaseAvailabilityType character varying(25);
UPDATE sdxcluster SET databaseAvailabilityType = 'HA' WHERE createdatabase = true;
UPDATE sdxcluster SET databaseAvailabilityType = 'NONE' WHERE createdatabase = false;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxcluster DROP COLUMN IF EXISTS databaseAvailabilityType;
