-- // CB-9224 Adding cloudbreak version to datalake DB
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS sdx_cluster_service_version VARCHAR(128) DEFAULT 'No Info';
UPDATE sdxcluster SET sdx_cluster_service_version = 'No Info' WHERE sdx_cluster_service_version IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxcluster DROP COLUMN IF EXISTS sdx_cluster_service_version;
