-- // CB-30785 Add remoteEnvironmentCrn field to CrossRealmTrust entity
-- Migration SQL that makes the change goes here.

ALTER TABLE crossrealmtrust ADD COLUMN IF NOT EXISTS remoteenvironmentcrn VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE crossrealmtrust DROP COLUMN IF EXISTS remoteenvironmentcrn;
