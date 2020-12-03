-- // CB-10140 add accountId to freeipa SaltSecurityConfig
-- Migration SQL that makes the change goes here.

ALTER TABLE saltsecurityconfig ADD COLUMN IF NOT EXISTS accountid VARCHAR(255);
ALTER TABLE saltsecurityconfig ALTER COLUMN accountid SET DEFAULT NULL;
UPDATE saltsecurityconfig SET accountid = (SELECT accountid FROM securityconfig WHERE securityconfig.saltsecurityconfig_id = saltsecurityconfig.id) WHERE accountid IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE saltsecurityconfig DROP COLUMN IF EXISTS accountid;

