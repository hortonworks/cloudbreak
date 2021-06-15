-- // CB-13096 Environment DB encryption_keyurl schema bug fix
-- Migration SQL that makes the change goes here.

ALTER TABLE environment_parameters ADD COLUMN IF NOT EXISTS encryption_keyurl text;


-- //@UNDO
-- SQL to undo the change goes here.

-- No undo steps this time; this script fixes the accidental & premature removal of the deprecated column "environment_parameters.encryption_keyurl"
-- that took place in CB-12118.
