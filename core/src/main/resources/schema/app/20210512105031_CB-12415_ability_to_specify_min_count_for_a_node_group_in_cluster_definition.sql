-- // CB-12415 ability to specify min count for a node group in cluster definition
-- Migration SQL that makes the change goes here.

ALTER TABLE instancegroup ADD COLUMN IF NOT EXISTS minimumnodecount INTEGER;
UPDATE instancegroup SET minimumNodeCount = 0 WHERE minimumnodecount IS NULL;
ALTER TABLE instancegroup ALTER minimumnodecount SET DEFAULT 0;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE instancegroup DROP COLUMN IF EXISTS minimumnodecount;