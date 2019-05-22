-- // CB-1420 Add CRN to databaseserverconfig
-- Migration SQL that makes the change goes here.

ALTER TABLE databaseserverconfig ADD COLUMN accountid TEXT;

ALTER TABLE databaseserverconfig ADD COLUMN resourcecrn TEXT;

UPDATE databaseserverconfig SET accountid = 'unknown' WHERE accountid IS NULL;

UPDATE databaseserverconfig SET resourcecrn = 'crn:altus:redbeams:us-west-1:unknown:databaseServer:unknown' WHERE resourcecrn IS NULL;

ALTER TABLE databaseserverconfig ALTER COLUMN accountid SET NOT NULL;

ALTER TABLE databaseserverconfig ALTER COLUMN resourcecrn SET NOT NULL;

CREATE INDEX IF NOT EXISTS databaseserverconfig_accountid_idx ON databaseserverconfig(accountid);

CREATE INDEX IF NOT EXISTS databaseserverconfig_resourcecrn_idx ON databaseserverconfig(resourcecrn);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS databaseserverconfig_resourcecrn_idx;

DROP INDEX IF EXISTS databaseserverconfig_accountid_idx;

ALTER TABLE databaseserverconfig DROP COLUMN resourcecrn;

ALTER TABLE databaseserverconfig DROP COLUMN accountid;
