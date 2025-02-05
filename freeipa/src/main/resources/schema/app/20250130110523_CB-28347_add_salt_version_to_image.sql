-- // CB-28347 Salt version upgrade on FreeIPA clusters
-- Migration SQL that makes the change goes here.

ALTER TABLE image ADD COLUMN IF NOT EXISTS saltversion varchar(255);
ALTER TABLE image_history ADD COLUMN IF NOT EXISTS saltversion varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE image DROP COLUMN IF EXISTS saltversion;
ALTER TABLE image_history DROP COLUMN IF EXISTS saltversion;