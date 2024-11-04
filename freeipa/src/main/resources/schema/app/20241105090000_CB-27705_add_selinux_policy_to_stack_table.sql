-- // CB-27705 Add selinux_policy to stack table
-- Migration SQL that makes the change goes here.

ALTER TABLE securityconfig ADD COLUMN IF NOT EXISTS selinux_policy varchar(255);
UPDATE securityconfig SET selinux_policy = 'PERMISSIVE' WHERE selinux_policy IS NULL;
ALTER TABLE securityconfig ALTER COLUMN selinux_policy SET DEFAULT 'PERMISSIVE';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE securityconfig DROP COLUMN IF EXISTS selinux_policy;
