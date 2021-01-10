-- // CB-10520 crate unique index for resources instead of constraint
-- Migration SQL that makes the change goes here.

ALTER TABLE resource DROP CONSTRAINT IF EXISTS uk_namebytypebystack;

DROP INDEX IF EXISTS uk_resource_where_stack_id_is_not_null;
DROP INDEX IF EXISTS uk_resource_where_stack_id_is_null;

CREATE UNIQUE INDEX uk_resource_where_stack_id_is_not_null ON resource (resourcename, resourcetype, resourcereference, resource_stack)
WHERE resource_stack IS NOT NULL;

CREATE UNIQUE INDEX uk_resource_where_stack_id_is_null ON resource (resourcename, resourcetype, resourcereference)
WHERE resource_stack IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS uk_resource_where_stack_id_is_not_null;
DROP INDEX IF EXISTS uk_resource_where_stack_id_is_null;

ALTER TABLE resource ADD CONSTRAINT uk_namebytypebystack UNIQUE (resourcename, resourcetype, resourcereference, resource_stack);
