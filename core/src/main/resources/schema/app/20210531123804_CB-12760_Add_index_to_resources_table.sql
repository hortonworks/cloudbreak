-- // CB-12760 Add index to resources table
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS idx_resource_stackid_resourcetype
    ON resource (resource_stack, resourcetype);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_resource_stackid_resourcetype;


