-- // CB-6267 Add AWS Spot parameters to Environment creation request
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS freeipaspotpercentage int;

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment DROP COLUMN IF EXISTS freeipaspotpercentage;
