-- // CLOUD-91592 handle parallel usage generation
-- Migration SQL that makes the change goes here.

DELETE FROM cloudbreakusage;
ALTER TABLE ONLY cloudbreakusage ADD CONSTRAINT uk_stack_instancegroup_day UNIQUE (stackid, instancegroup, day);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ONLY cloudbreakusage DROP CONSTRAINT IF EXISTS uk_stack_instancegroup_day;
