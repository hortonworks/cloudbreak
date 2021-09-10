-- // CB-13685 alter image table add date column
-- Migration SQL that makes the change goes here.

ALTER TABLE image ADD COLUMN IF NOT EXISTS image_date VARCHAR(255);
ALTER TABLE image_history ADD COLUMN IF NOT EXISTS image_date VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE image DROP COLUMN IF EXISTS image_date;
ALTER TABLE image_history DROP COLUMN IF EXISTS image_date;