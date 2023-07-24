-- // CB-21369 Remove unnecessary sdxcluster_id index from sdxdatabase
-- Migration SQL that makes the change goes here.

DROP INDEX IF EXISTS unq_index_sdxdatabase_sdxcluster_id;

-- //@UNDO
-- SQL to undo the change goes here.
