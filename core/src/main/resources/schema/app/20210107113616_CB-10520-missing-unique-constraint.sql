-- // CB-10520-missing-unique-constraint
-- Migration SQL that makes the change goes here.
ALTER TABLE resource DROP CONSTRAINT IF EXISTS uk_namebytypebystack;

ALTER TABLE resource ADD CONSTRAINT uk_namebytypebystack UNIQUE (resourcename, resourcetype, resourcereference, resource_stack);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE resource DROP CONSTRAINT IF EXISTS uk_namebytypebystack;