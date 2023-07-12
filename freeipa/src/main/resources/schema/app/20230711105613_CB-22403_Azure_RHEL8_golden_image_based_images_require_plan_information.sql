-- // CB-22403 Azure RHEL8 golden image based images require plan information
-- Migration SQL that makes the change goes here.

ALTER TABLE image ADD COLUMN IF NOT EXISTS sourceimage VARCHAR(255);
ALTER TABLE image_history ADD COLUMN IF NOT EXISTS sourceimage VARCHAR(255);


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE image DROP COLUMN IF EXISTS sourceimage;
ALTER TABLE image_history DROP COLUMN IF EXISTS sourceimage;