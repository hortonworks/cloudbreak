-- // BUG-108475_blueprint_v3
-- Migration SQL that makes the change goes here.

ALTER TABLE blueprint ALTER COLUMN owner DROP NOT NULL;
ALTER TABLE blueprint ALTER COLUMN account DROP NOT NULL;
ALTER TABLE blueprint DROP CONSTRAINT IF EXISTS uk_blueprint_account_name;

-- //@UNDO
-- SQL to undo the change goes here.
