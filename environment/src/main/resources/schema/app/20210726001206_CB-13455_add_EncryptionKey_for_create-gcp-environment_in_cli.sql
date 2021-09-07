-- // CB-13455_add_EncryptionKey_for_create-gcp-environment_in_cli
-- Migration SQL that makes the change goes here.
ALTER TABLE environment_parameters ADD COLUMN IF NOT EXISTS encryption_key text;


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment_parameters DROP COLUMN IF EXISTS encryption_key;

