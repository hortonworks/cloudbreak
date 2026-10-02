-- // CB-34298 Store the Azure Hyper-V generation image tag for FreeIPA images
-- Migration SQL that makes the change goes here.

ALTER TABLE image ADD COLUMN IF NOT EXISTS hypervgenerationtag varchar(255);
ALTER TABLE image_history ADD COLUMN IF NOT EXISTS hypervgenerationtag varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE image DROP COLUMN IF EXISTS hypervgenerationtag;
ALTER TABLE image_history DROP COLUMN IF EXISTS hypervgenerationtag;
