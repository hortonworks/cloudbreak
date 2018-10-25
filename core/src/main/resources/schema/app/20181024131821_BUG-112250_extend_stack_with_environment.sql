-- // BUG-112250 extend stack with environment
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN IF NOT EXISTS environment_id bigint;
ALTER TABLE ONLY stack ADD CONSTRAINT fk_stack_environment FOREIGN KEY (environment_id) REFERENCES environment(id);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ONLY stack DROP CONSTRAINT IF EXISTS fk_stack_environment;
ALTER TABLE stack DROP COLUMN IF EXISTS environment_id;
