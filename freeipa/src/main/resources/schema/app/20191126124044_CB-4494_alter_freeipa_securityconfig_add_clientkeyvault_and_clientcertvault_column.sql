-- // CB-4494 alter freeipa securityconfig add clientkeyvault and clientcertvault column
-- Migration SQL that makes the change goes here.

ALTER TABLE securityconfig ADD COLUMN IF NOT EXISTS clientcertvault TEXT;
ALTER TABLE securityconfig ADD COLUMN IF NOT EXISTS clientkeyvault TEXT;
ALTER TABLE securityconfig ADD COLUMN IF NOT EXISTS accountid VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE securityconfig DROP COLUMN IF EXISTS clientcertvault;
ALTER TABLE securityconfig DROP COLUMN IF EXISTS clientkeyvault;
ALTER TABLE securityconfig DROP COLUMN IF EXISTS accountid;
