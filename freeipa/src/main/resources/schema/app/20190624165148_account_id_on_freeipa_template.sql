-- // account id on freeipa template
-- Migration SQL that makes the change goes here.

ALTER TABLE template ADD COLUMN accountId VARCHAR(255);
UPDATE template t1 SET accountId = (SELECT s.accountid FROM stack s JOIN instancegroup ig ON s.id=ig.stack_id JOIN template t2 ON ig.template_id=t2.id WHERE t2.id=t1.id);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE template DROP COLUMN IF EXISTS accountId;

