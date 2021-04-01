-- // CB-11872 Implement ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_STARTED_STATE - to create the DES for Azure
-- Migration SQL that makes the change goes here.
ALTER TABLE environment_parameters ADD COLUMN IF NOT EXISTS disk_encryption_set_id text;


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment_parameters DROP COLUMN IF EXISTS `disk_encryption_set_id`;

