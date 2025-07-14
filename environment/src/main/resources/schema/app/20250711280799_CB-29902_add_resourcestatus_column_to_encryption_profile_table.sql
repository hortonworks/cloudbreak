-- // CB-29902: Add resourcestatus column to encryption profile table
-- Migration SQL that makes the change goes here.

ALTER TABLE encryptionprofile ADD COLUMN IF NOT EXISTS resourcestatus VARCHAR(255) NOT NULL DEFAULT 'USER_MANAGED';

-- This ensures that only one encryption profile can have the resourcestatus 'DEFAULT' per account.
CREATE UNIQUE INDEX IF NOT EXISTS encryptionprofile_accountid_resourcestatus_default_idx
    ON encryptionprofile(accountid)
    WHERE resourcestatus = 'DEFAULT';
UPDATE encryptionprofile SET resourcestatus = 'USER_MANAGED' WHERE resourcestatus IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS encryptionprofile_accountid_resourcestatus_default_idx;
ALTER TABLE encryptionprofile DROP COLUMN IF EXISTS resourcestatus;