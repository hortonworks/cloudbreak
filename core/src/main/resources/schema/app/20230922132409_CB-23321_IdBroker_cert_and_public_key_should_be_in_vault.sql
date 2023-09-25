-- // CB-23321 IdBroker cert and public key should be in vault
-- Migration SQL that makes the change goes here.

ALTER TABLE idbroker ADD COLUMN IF NOT EXISTS signcertsecret TEXT;
ALTER TABLE idbroker ADD COLUMN IF NOT EXISTS signpubsecret TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE idbroker DROP COLUMN IF EXISTS signcertsecret;
ALTER TABLE idbroker DROP COLUMN IF EXISTS signpubsecret;
