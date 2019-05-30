-- // CB-1653 Adjust database CRN storage
-- Migration SQL that makes the change goes here.

ALTER TABLE databaseconfig ALTER COLUMN crn TYPE TEXT;

ALTER TABLE databaseconfig ADD COLUMN accountid TEXT;

UPDATE databaseconfig SET accountid = 'unknown' WHERE accountid IS NULL;

ALTER TABLE databaseconfig ALTER COLUMN accountid SET NOT NULL;

CREATE INDEX IF NOT EXISTS databaseconfig_accountid_idx ON databaseconfig(accountid);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS databaseconfig_accountid_idx;

ALTER TABLE databaseconfig DROP COLUMN accountid;

ALTER TABLE databaseconfig ALTER COLUMN crn TYPE VARCHAR(255);

