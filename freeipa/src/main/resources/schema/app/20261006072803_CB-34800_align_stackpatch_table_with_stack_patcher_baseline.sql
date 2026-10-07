-- // CB-34800 align stackpatch table with the stack-patcher baseline
-- Migration SQL that makes the change goes here.

ALTER TABLE ONLY stackpatch DROP CONSTRAINT IF EXISTS fk_stackpatch_stack_id;
ALTER TABLE ONLY stackpatch RENAME CONSTRAINT uk_stackpatch_stack_type TO uk_stackpatch_resource_type;
DROP INDEX IF EXISTS stackpatch_type_stack;
ALTER TABLE ONLY stackpatch RENAME COLUMN stack_id TO resource_id;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ONLY stackpatch RENAME COLUMN resource_id TO stack_id;
CREATE INDEX IF NOT EXISTS stackpatch_type_stack ON stackpatch(stack_id, type);
ALTER TABLE ONLY stackpatch RENAME CONSTRAINT uk_stackpatch_resource_type TO uk_stackpatch_stack_type;
ALTER TABLE ONLY stackpatch ADD CONSTRAINT fk_stackpatch_stack_id FOREIGN KEY (stack_id) REFERENCES stack(id);
