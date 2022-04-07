-- // CB-16612 adding original name
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN originalname text;
UPDATE environment SET originalname=environment.name WHERE originalname IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS originalname;


