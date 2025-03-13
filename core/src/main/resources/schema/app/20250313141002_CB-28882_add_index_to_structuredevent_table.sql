-- // CB-28882 Add index to structuredevent table to improve query performance
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS idx_structuredevent_workspace_resourcecrn_timestamp ON structuredevent (workspace_id, resourcecrn, "timestamp" DESC);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_structuredevent_workspace_resourcecrn_timestamp;