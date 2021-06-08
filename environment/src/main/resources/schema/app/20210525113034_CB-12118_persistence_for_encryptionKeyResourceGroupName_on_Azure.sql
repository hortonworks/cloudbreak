-- // CB-12118 persistence for encryptionKeyResourceGroupName on Azure
-- Migration SQL that makes the change goes here.
ALTER TABLE environment_parameters DROP COLUMN IF EXISTS encryption_keyurl;
ALTER TABLE environment_parameters ADD COLUMN IF NOT EXISTS encryption_key_resource_group_name varchar(255);


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment_parameters DROP COLUMN IF EXISTS encryption_key_resource_group_name;
ALTER TABLE environment_parameters ADD COLUMN IF NOT EXISTS encryption_keyurl text;

