-- // CB-31272 Change the encryption profile handling from name to CRN
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN IF NOT EXISTS notificationstate VARCHAR(255);
UPDATE stack SET notificationstate = 'ENABLED' WHERE notificationstate IS NULL;
ALTER TABLE stack ALTER COLUMN notificationstate SET DEFAULT 'ENABLED';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS notificationstate;