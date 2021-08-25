-- // CB-13110:Add the encryption Arn parameter passed from CLI to AWS env creation in Cloudbreak
-- Migration SQL that makes the change goes here.
ALTER TABLE environment_parameters ADD COLUMN IF NOT EXISTS encryption_key_arn VARCHAR(255);


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment_parameters DROP COLUMN IF EXISTS encryption_key_arn;
