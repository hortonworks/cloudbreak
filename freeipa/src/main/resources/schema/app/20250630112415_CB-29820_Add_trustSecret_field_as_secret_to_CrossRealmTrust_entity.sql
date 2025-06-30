-- // CB-29820 Add trustSecret field as secret to CrossRealmTrust entity
-- Migration SQL that makes the change goes here.

ALTER TABLE crossrealmtrust ADD COLUMN IF NOT EXISTS trustsecret TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE crossrealmtrust DROP COLUMN IF EXISTS trustsecret;
