-- // CLOUD-76776 Cloudbreak support SmartSense and Flex info as individual entity, extend flex
-- Migration SQL that makes the change goes here.

ALTER TABLE flexsubscription ADD COLUMN isDefault boolean default false;
ALTER TABLE flexsubscription ADD COLUMN usedForController boolean default false;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE flexsubscription DROP COLUMN IF EXISTS isDefault;
ALTER TABLE flexsubscription DROP COLUMN IF EXISTS usedForController;
