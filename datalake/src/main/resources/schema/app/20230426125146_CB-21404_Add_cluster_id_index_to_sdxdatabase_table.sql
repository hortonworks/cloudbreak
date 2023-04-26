-- // CB-21404 Add cluster_id index to sdxdatabase table
-- Migration SQL that makes the change goes here.

CREATE UNIQUE INDEX IF NOT EXISTS unq_index_sdxdatabase_sdxcluster_id ON sdxdatabase(sdxcluster_id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS unq_index_sdxdatabase_sdxcluster_id;
