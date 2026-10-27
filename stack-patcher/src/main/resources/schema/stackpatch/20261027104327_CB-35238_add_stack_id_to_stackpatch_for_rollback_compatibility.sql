-- // CB-35238 add stack_id back to stackpatch so that services of the previous release can still use the table
-- Migration SQL that makes the change goes here.

ALTER TABLE stackpatch ADD COLUMN IF NOT EXISTS stack_id bigint;
UPDATE stackpatch SET stack_id = resource_id WHERE stack_id IS NULL;
ALTER TABLE stackpatch ALTER COLUMN resource_id DROP NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_stackpatch_stack_id_type ON stackpatch (stack_id, type);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS uk_stackpatch_stack_id_type;
UPDATE stackpatch SET resource_id = stack_id WHERE resource_id IS NULL;
ALTER TABLE stackpatch ALTER COLUMN resource_id SET NOT NULL;
ALTER TABLE stackpatch DROP COLUMN IF EXISTS stack_id;
