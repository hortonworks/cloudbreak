-- // RMP-12882_missing_descriptions
-- Migration SQL that makes the change goes here.

ALTER TABLE imagecatalog ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE kubernetesconfig ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE rdsconfig ADD COLUMN IF NOT EXISTS description TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE imagecatalog DROP COLUMN IF EXISTS description;
ALTER TABLE kubernetesconfig DROP COLUMN IF EXISTS description;
ALTER TABLE rdsconfig DROP COLUMN IF EXISTS description;