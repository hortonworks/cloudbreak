-- // CB-21472 distrox audit event API very slow query "JDBC Execution Statement time warning (>1000ms): 4086ms"
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS idx_structuredevent_eventtype_resourcetype_resourceid_timestamp ON structuredevent (eventtype, resourcetype, resourceid, timestamp desc);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_structuredevent_eventtype_resourcetype_resourceid_timestamp;
