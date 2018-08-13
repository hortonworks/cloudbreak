-- // BUG-108475
-- Migration SQL that makes the change goes here.

ALTER TABLE rdsconfig ALTER COLUMN owner DROP NOT NULL;
ALTER TABLE rdsconfig ALTER COLUMN account DROP NOT NULL;
ALTER TABLE rdsconfig DROP CONSTRAINT IF EXISTS uk_rdsconfig_account_name;

ALTER TABLE stack
    ADD createdby int8,
    ADD CONSTRAINT fk_stack_createdby FOREIGN KEY (createdby) REFERENCES users(id);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP CONSTRAINT IF EXISTS fk_rdsconfig_createdby;

UPDATE stack
SET createdby = null
WHERE createdby is not null;