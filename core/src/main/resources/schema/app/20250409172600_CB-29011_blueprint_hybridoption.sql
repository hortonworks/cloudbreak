-- // CB-28780 set loadbalancer sku to basic if empty
-- Migration SQL that makes the change goes here.

ALTER TABLE blueprint ADD COLUMN IF NOT EXISTS hybridoption VARCHAR(255);
UPDATE blueprint SET hybridoption = 'NONE' WHERE hybridoption IS NULL;
ALTER TABLE blueprint ALTER COLUMN hybridoption SET DEFAULT 'NONE';
ALTER TABLE blueprint ALTER COLUMN hybridoption SET NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE blueprint DROP COLUMN IF EXISTS hybridoption;
