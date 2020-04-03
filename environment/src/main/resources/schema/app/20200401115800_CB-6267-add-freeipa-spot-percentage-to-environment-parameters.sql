-- // CB-6267 Add AWS Spot parameters to Environment creation request
-- Migration SQL that makes the change goes here.

ALTER TABLE environment_parameters ADD COLUMN IF NOT EXISTS freeipa_spot_percentage int;
UPDATE environment_parameters SET freeipa_spot_percentage = 0 WHERE parameters_platform = 'AWS' AND freeipa_spot_percentage IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment_parameters DROP COLUMN IF EXISTS freeipa_spot_percentage;
