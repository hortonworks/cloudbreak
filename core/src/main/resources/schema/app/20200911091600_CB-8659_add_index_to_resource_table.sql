-- // CB-7808 set from datahub, datalake stack type to stacks in structured events
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS idx_resource_stackid_resourcename_resourcetype
    ON resource (resource_stack, resourcename, resourcetype);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_resource_stackid_resourcename_resourcetype;