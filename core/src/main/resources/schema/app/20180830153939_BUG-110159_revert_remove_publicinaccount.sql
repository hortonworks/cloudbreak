-- // BUG-110159
-- Migration SQL that makes the change goes here.

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


ALTER TABLE blueprint ALTER COLUMN publicinaccount SET DEFAULT false;
ALTER TABLE clustertemplate ALTER COLUMN publicinaccount SET DEFAULT false;
ALTER TABLE constrainttemplate ALTER COLUMN publicinaccount SET DEFAULT false;
ALTER TABLE credential ALTER COLUMN publicinaccount SET DEFAULT false;
ALTER TABLE filesystem ALTER COLUMN publicinaccount SET DEFAULT false;
ALTER TABLE flexsubscription ALTER COLUMN publicinaccount SET DEFAULT false;
ALTER TABLE imagecatalog ALTER COLUMN publicinaccount SET DEFAULT false;
ALTER TABLE ldapconfig ALTER COLUMN publicinaccount SET DEFAULT false;
ALTER TABLE network ALTER COLUMN publicinaccount SET DEFAULT false;
ALTER TABLE managementpack ALTER COLUMN publicinaccount SET DEFAULT false;
ALTER TABLE proxyconfig ALTER COLUMN publicinaccount SET DEFAULT false;
ALTER TABLE rdsconfig ALTER COLUMN publicinaccount SET DEFAULT false;
ALTER TABLE recipe ALTER COLUMN publicinaccount SET DEFAULT false;
ALTER TABLE smartsensesubscription ALTER COLUMN publicinaccount SET DEFAULT false;
ALTER TABLE stack ALTER COLUMN publicinaccount SET DEFAULT false;
ALTER TABLE securitygroup ALTER COLUMN publicinaccount SET DEFAULT false;
ALTER TABLE template ALTER COLUMN publicinaccount SET DEFAULT false;
ALTER TABLE topology ALTER COLUMN publicinaccount SET DEFAULT false;

-- //@UNDO
-- SQL to undo the change goes here.


