-- // CB-29105 Add encryption profile to Environment
-- Migration SQL that makes the change goes here.

ALTER TABLE environment
    ADD COLUMN IF NOT EXISTS encryption_profile_id BIGINT NULL;

ALTER TABLE ONLY environment ADD CONSTRAINT fk_encryption_profile_id
    FOREIGN KEY (encryption_profile_id) REFERENCES encryptionprofile(id);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP CONSTRAINT IF EXISTS fk_encryption_profile_id;

ALTER TABLE environment DROP COLUMN IF EXISTS encryption_profile_id;