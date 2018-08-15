-- // BUG-108475 mpack auth
-- Migration SQL that makes the change goes here.

ALTER TABLE managementpack ALTER COLUMN owner DROP NOT NULL;
ALTER TABLE managementpack ALTER COLUMN account DROP NOT NULL;
ALTER TABLE managementpack DROP CONSTRAINT IF EXISTS uk_managementpack_account_name;

-- //@UNDO
-- SQL to undo the change goes here.


