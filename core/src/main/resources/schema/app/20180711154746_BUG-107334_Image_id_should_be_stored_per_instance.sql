-- // BUG-107334 Image id should be stored per instance
-- Migration SQL that makes the change goes here.
ALTER TABLE instancemetadata ADD COLUMN IF NOT EXISTS imageId VARCHAR(40);

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE instancemetadata DROP COLUMN IF EXISTS imageId;


