-- // CB-2980 Store base location on sdx side
-- Migration SQL that makes the change goes here.


ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS cloudStorageBaseLocation TEXT;
ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS cloudStorageFileSystemType VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxcluster DROP COLUMN IF EXISTS cloudStorageBaseLocation;
ALTER TABLE sdxcluster DROP COLUMN IF EXISTS cloudStorageFileSystemType;
