-- // CB-34416 Drop Encryption Profile name for Datalake / DataHub
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS encryption_profile_name;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster
    ADD COLUMN IF NOT EXISTS encryption_profile_name varchar(255) NULL;