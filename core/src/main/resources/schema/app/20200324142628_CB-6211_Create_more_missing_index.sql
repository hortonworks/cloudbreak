-- // CB-6211 Create more missing index
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS idx_resource_resourcestack on resource(resource_stack);
CREATE INDEX IF NOT EXISTS idx_instancegroup_templateid on instancegroup(template_id) where template_id is not null;
CREATE INDEX IF NOT EXISTS idx_flowlog_flowchainid on flowlog(flowchainid) where flowchainid is not null;

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_resource_resourcestack;
DROP INDEX IF EXISTS idx_instancegroup_templateid;
DROP INDEX IF EXISTS idx_flowlog_flowchainid;
