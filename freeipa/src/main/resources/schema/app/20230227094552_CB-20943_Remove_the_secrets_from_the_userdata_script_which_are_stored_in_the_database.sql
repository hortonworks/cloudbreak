-- // CB-20943 Remove the secrets from the userdata script which are stored in the database
-- Migration SQL that makes the change goes here.

ALTER TABLE image ADD COLUMN IF NOT EXISTS gatewayuserdata TEXT;
ALTER TABLE image ADD COLUMN IF NOT EXISTS accountid VARCHAR(255);

ALTER TABLE image_history ADD COLUMN IF NOT EXISTS gatewayuserdata TEXT;
ALTER TABLE image_history ADD COLUMN IF NOT EXISTS accountid VARCHAR(255);

UPDATE image i SET accountid = s.accountid FROM stack s WHERE s.id = i.stack_id;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE image DROP COLUMN IF EXISTS gatewayuserdata;
ALTER TABLE image DROP COLUMN IF EXISTS accountid;

ALTER TABLE image_history DROP COLUMN IF EXISTS gatewayuserdata;
ALTER TABLE image_history DROP COLUMN IF EXISTS accountid;

