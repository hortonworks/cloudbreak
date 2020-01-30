-- // CB-5121 Add parent environment
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS parent_environment_id BIGINT NULL;

ALTER TABLE ONLY environment ADD CONSTRAINT fk_environment_parent_environment_id
  FOREIGN KEY (parent_environment_id) REFERENCES environment(id);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP CONSTRAINT IF EXISTS fk_environment_parent_environment_id;

ALTER TABLE environment DROP COLUMN IF EXISTS parent_environment_id;