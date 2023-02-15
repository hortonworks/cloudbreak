-- // CB-20941 Slow query LegacyPagingStructuredEventRepository. findByEventTypeAndResourceTypeAndResourceId, due to missing index
-- Migration SQL that makes the change goes here.

DROP INDEX IF EXISTS idx_structuredevent_userid_eventtype_resourcetype_resourceid;
CREATE INDEX IF NOT EXISTS idx_structuredevent_eventtype_resourcetype_resourceid ON structuredevent (eventtype, resourcetype, resourceid);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_structuredevent_eventtype_resourcetype_resourceid;
CREATE INDEX idx_structuredevent_userid_eventtype_resourcetype_resourceid ON structuredevent (userid, eventtype, resourcetype, resourceid);
