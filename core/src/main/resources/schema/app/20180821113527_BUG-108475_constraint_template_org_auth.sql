-- // BUG-108475
-- Migration SQL that makes the change goes here.

ALTER TABLE constrainttemplate ALTER COLUMN owner DROP NOT NULL;
ALTER TABLE constrainttemplate ALTER COLUMN account DROP NOT NULL;
ALTER TABLE constrainttemplate ALTER COLUMN publicinaccount DROP NOT NULL;
ALTER TABLE constrainttemplate DROP CONSTRAINT IF EXISTS uk_constrainttemplate_account_name;

-- //@UNDO
-- SQL to undo the change goes here.


