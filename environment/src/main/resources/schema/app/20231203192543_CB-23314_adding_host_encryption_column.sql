-- // CB-23314 adding host encryption column
-- Migration SQL that makes the change goes here.

ALTER TABLE environment_parameters ADD COLUMN IF NOT EXISTS enable_host_encryption boolean NOT NULL DEFAULT false;

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment_parameters DROP COLUMN IF EXISTS enable_host_encryption;


