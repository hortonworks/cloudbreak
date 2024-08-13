-- // CB-26863 introduce userCrn into the structured events
-- Migration SQL that makes the change goes here.

ALTER TABLE structuredevent ADD COLUMN userCrn VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE structuredevent DROP COLUMN userCrn;
