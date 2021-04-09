-- // CB-11874: Make DiskEncryptionSetId available in environment service API response
-- Migration SQL that makes the change goes here.
ALTER TABLE environment_parameters ADD COLUMN IF NOT EXISTS encryption_key_url text;
ALTER TABLE environment_parameters ADD COLUMN IF NOT EXISTS disk_encryption_set_id text;



-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment_parameters DROP COLUMN IF EXISTS disk_encryption_set_id;
ALTER TABLE environment_parameters DROP COLUMN IF EXISTS encryption_key_url;

