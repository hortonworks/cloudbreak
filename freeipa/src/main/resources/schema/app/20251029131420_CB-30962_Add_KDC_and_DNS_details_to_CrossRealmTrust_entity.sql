-- // CB-30962 Add KDC and DNS details to CrossRealmTrust entity
-- Migration SQL that makes the change goes here.

ALTER TABLE crossrealmtrust ADD COLUMN IF NOT EXISTS kdctype VARCHAR(255);
UPDATE crossrealmtrust SET kdctype = 'ACTIVE_DIRECTORY' WHERE kdctype IS NULL;
ALTER TABLE crossrealmtrust ALTER COLUMN kdctype SET NOT NULL;

ALTER TABLE crossrealmtrust ADD COLUMN IF NOT EXISTS dnsip VARCHAR(255);
UPDATE crossrealmtrust SET dnsip = ip WHERE dnsip IS NULL;
ALTER TABLE crossrealmtrust ALTER COLUMN dnsip SET NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE crossrealmtrust DROP COLUMN IF EXISTS kdctype;

ALTER TABLE crossrealmtrust DROP COLUMN IF EXISTS dnsip;
