-- // CB-24851 Add imdsversion column to image table
-- Migration SQL that makes the change goes here.

ALTER TABLE image ADD COLUMN IF NOT EXISTS imdsversion VARCHAR(255);
ALTER TABLE image_history ADD COLUMN IF NOT EXISTS imdsversion VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE image DROP COLUMN IF EXISTS imdsversion;
ALTER TABLE image_history DROP COLUMN IF EXISTS imdsversion;
