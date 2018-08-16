-- // BUG-108475 credential resource change
-- Migration SQL that makes the change goes here.
ALTER TABLE credential ALTER COLUMN owner DROP NOT NULL;
ALTER TABLE credential ALTER COLUMN account DROP NOT NULL;
ALTER TABLE credential DROP CONSTRAINT IF EXISTS uk_credential_account_name;


-- //@UNDO
-- SQL to undo the change goes here.