-- // CB-16699 Bring your own DNS zone - prepare private DNS zone endpoint name multiple private DNS zones
-- Migration SQL that makes the change goes here.
ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS databaseprivatednszoneid VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_network DROP COLUMN IF EXISTS databaseprivatednszoneid;
