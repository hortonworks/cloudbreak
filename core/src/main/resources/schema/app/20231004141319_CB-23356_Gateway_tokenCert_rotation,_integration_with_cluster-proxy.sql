-- // CB-23356 Gateway tokenCert rotation, integration with cluster-proxy
-- Migration SQL that makes the change goes here.

ALTER TABLE gateway ADD COLUMN IF NOT EXISTS tokencertsecret TEXT;
ALTER TABLE gateway ADD COLUMN IF NOT EXISTS tokenpubsecret TEXT;
ALTER TABLE gateway ADD COLUMN IF NOT EXISTS tokenkeysecret TEXT;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE gateway DROP COLUMN IF EXISTS tokencertsecret;
ALTER TABLE gateway DROP COLUMN IF EXISTS tokenpubsecret;
ALTER TABLE gateway DROP COLUMN IF EXISTS tokenkeysecret;

