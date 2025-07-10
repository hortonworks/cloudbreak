-- // CB-29899 Add trustStatus field to CrossRealmTrust entity
-- Migration SQL that makes the change goes here.

ALTER TABLE crossrealmtrust ADD COLUMN IF NOT EXISTS operationid VARCHAR(255);
ALTER TABLE crossrealmtrust ADD COLUMN IF NOT EXISTS truststatus VARCHAR(255);
UPDATE crossrealmtrust SET truststatus = 'UNKNOWN' WHERE truststatus IS NULL;
ALTER TABLE crossrealmtrust ALTER COLUMN truststatus SET DEFAULT 'UNKNOWN';
ALTER TABLE crossrealmtrust ALTER COLUMN truststatus SET NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE crossrealmtrust DROP COLUMN IF EXISTS operationid;
ALTER TABLE crossrealmtrust DROP COLUMN IF EXISTS truststatus;
