-- // CB-31272 Change the encryption profile handling from name to CRN
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster
    ADD COLUMN IF NOT EXISTS encryption_profile_crn varchar(255) NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS encryption_profile_crn;