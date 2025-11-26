-- // CB-31272 Change the encryption profile handling from name to CRN
-- Migration SQL that makes the change goes here.

ALTER TABLE environment
    ADD COLUMN IF NOT EXISTS encryption_profile_crn varchar(255) NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS encryption_profile_crn;