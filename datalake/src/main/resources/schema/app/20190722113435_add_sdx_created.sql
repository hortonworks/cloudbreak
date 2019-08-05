-- // CB-2541 Add creation date to SdxClusterResponse
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS created bigint;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxcluster DROP COLUMN IF EXISTS created;