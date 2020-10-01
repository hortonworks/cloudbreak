-- // CB-7928 Add cloud platform parameter to sdx cluster
-- Migration SQL that makes the change goes here.
ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS cloud_platform boolean;
ALTER TABLE sdxcluster ALTER cloud_platform SET DEFAULT null;


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE sdxcluster DROP COLUMN IF EXISTS cloud_platform;
