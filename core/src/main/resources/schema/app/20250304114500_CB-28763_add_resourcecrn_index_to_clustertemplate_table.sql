-- // CB-28763 Create index on clustertemplate(resourcecrn)
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS idx_clustertemplate_resourcecrn ON clustertemplate (resourcecrn);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_clustertemplate_resourcecrn;