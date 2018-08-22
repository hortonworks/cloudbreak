-- // BUG-109647_remove_publicinaccount
-- Migration SQL that makes the change goes here.

ALTER TABLE blueprint DROP COLUMN IF EXISTS publicinaccount;
ALTER TABLE clustertemplate DROP COLUMN IF EXISTS publicinaccount;
ALTER TABLE constrainttemplate DROP COLUMN IF EXISTS publicinaccount;
ALTER TABLE credential DROP COLUMN IF EXISTS publicinaccount;
ALTER TABLE filesystem DROP COLUMN IF EXISTS publicinaccount;
ALTER TABLE flexsubscription DROP COLUMN IF EXISTS publicinaccount;
ALTER TABLE imagecatalog DROP COLUMN IF EXISTS publicinaccount;
ALTER TABLE ldapconfig DROP COLUMN IF EXISTS publicinaccount;
ALTER TABLE network DROP COLUMN IF EXISTS publicinaccount;
ALTER TABLE managementpack DROP COLUMN IF EXISTS publicinaccount;
ALTER TABLE proxyconfig DROP COLUMN IF EXISTS publicinaccount;
ALTER TABLE rdsconfig DROP COLUMN IF EXISTS publicinaccount;
ALTER TABLE recipe DROP COLUMN IF EXISTS publicinaccount;
ALTER TABLE smartsensesubscription DROP COLUMN IF EXISTS publicinaccount;
ALTER TABLE stack DROP COLUMN IF EXISTS publicinaccount;
ALTER TABLE securitygroup DROP COLUMN IF EXISTS publicinaccount;
ALTER TABLE template DROP COLUMN IF EXISTS publicinaccount;
ALTER TABLE topology DROP COLUMN IF EXISTS publicinaccount;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE blueprint ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE clustertemplate ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE constrainttemplate ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE credential ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE filesystem ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE flexsubscription ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE imagecatalog ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE ldapconfig ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE network ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE managementpack ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE proxyconfig ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE rdsconfig ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE recipe ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE smartsensesubscription ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE stack ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE securitygroup ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE template ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE topology ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;