-- // CB-13711 create index on instance metadata for status and instance group based queries
-- Migration SQL that makes the change goes here.
CREATE INDEX IF NOT EXISTS idx_instancemetadata_instancestatus_instancegroupid
    ON instancemetadata (instancestatus, instancegroup_id);


-- //@UNDO
-- SQL to undo the change goes here.
DROP INDEX IF EXISTS idx_instancemetadata_instancestatus_instancegroupid;

