-- // CB-20506 alter instancemetadata table add image column
-- Migration SQL that makes the change goes here.

ALTER TABLE instancemetadata ADD COLUMN IF NOT EXISTS image TEXT;

-- //@UNDO
-- SQL to undo the change goes here.


ALTER TABLE instancemetadata DROP COLUMN IF EXISTS image;