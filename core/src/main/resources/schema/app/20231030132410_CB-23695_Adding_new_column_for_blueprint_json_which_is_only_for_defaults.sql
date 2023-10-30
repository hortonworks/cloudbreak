-- // CB-23695 Adding new column for blueprint json which is only for defaults
-- Migration SQL that makes the change goes here.

ALTER TABLE blueprint ADD COLUMN IF NOT EXISTS defaultBlueprintText TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE blueprint DROP COLUMN IF EXISTS defaultBlueprintText;
