-- // CB-11152: Add keyUrl CLI param in Azure environment creation
-- Migration SQL that makes the change goes here.
ALTER TABLE environment_parameters ADD COLUMN IF NOT EXISTS encryption_keyUrl text;

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment_parameters DROP COLUMN IF EXISTS encryption_keyUrl;