-- // CB-31272 Change the encryption profile handling from name to CRN
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS notificationstate VARCHAR(255);
UPDATE sdxcluster SET notificationstate = 'ENABLED' WHERE notificationstate IS NULL;
ALTER TABLE sdxcluster ALTER COLUMN notificationstate SET DEFAULT 'ENABLED';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxcluster DROP COLUMN IF EXISTS notificationstate;