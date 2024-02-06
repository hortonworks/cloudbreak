-- // CB-24726 Add CMEK with user managed identity option to environment service
-- Migration SQL that makes the change goes here.

ALTER TABLE environment_parameters ADD COLUMN IF NOT EXISTS user_managed_identity text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_parameters DROP COLUMN IF EXISTS user_managed_identity;
