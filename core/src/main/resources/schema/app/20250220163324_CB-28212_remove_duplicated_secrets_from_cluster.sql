-- // CB-28212 Remove obsolete/duplicated secrets from Vault and RDS
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS cloudbreakAmbariUser;
ALTER TABLE cluster DROP COLUMN IF EXISTS cloudbreakAmbariPassword;
ALTER TABLE cluster DROP COLUMN IF EXISTS dpAmbariUser;
ALTER TABLE cluster DROP COLUMN IF EXISTS dpAmbariPassword;
ALTER TABLE cluster DROP COLUMN IF EXISTS ambariSecurityMasterKey;
ALTER TABLE cluster DROP COLUMN IF EXISTS clusterManagerSecurityMasterKey;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS cloudbreakAmbariUser TEXT;
ALTER TABLE cluster ADD COLUMN IF NOT EXISTS cloudbreakAmbariPassword TEXT;
ALTER TABLE cluster ADD COLUMN IF NOT EXISTS dpAmbariUser TEXT;
ALTER TABLE cluster ADD COLUMN IF NOT EXISTS dpAmbariPassword TEXT;
ALTER TABLE cluster ADD COLUMN IF NOT EXISTS ambariSecurityMasterKey TEXT;
ALTER TABLE cluster ADD COLUMN IF NOT EXISTS clusterManagerSecurityMasterKey TEXT;
