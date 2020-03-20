-- // CB-6176 Fix sdx rescheduling
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS idx_sdxcluster_id_stackcrn ON sdxcluster(id, stackcrn) WHERE deleted IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_sdxcluster_id_stackcrn;
