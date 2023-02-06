-- // CB-20463 Adds java version field to stack
-- Migration SQL that makes the change goes here.
ALTER TABLE stack ADD COLUMN IF NOT EXISTS javaversion INTEGER;


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE stack DROP COLUMN IF EXISTS javaversion;
