-- // CB-13711 create index on instancegroup_availabilityzones by instancegroup_id to mitigate database CPU usage
-- Migration SQL that makes the change goes here.
CREATE INDEX IF NOT EXISTS idx_instancegroup_availabilityzones_instancegroupid
    ON instancegroup_availabilityzones (instancegroup_id);


-- //@UNDO
-- SQL to undo the change goes here.
DROP INDEX IF EXISTS idx_instancegroup_availabilityzones_instancegroupid;



