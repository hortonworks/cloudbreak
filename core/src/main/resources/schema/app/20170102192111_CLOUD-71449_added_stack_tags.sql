-- // CLOUD-71449 added stack tags
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN tags text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN tags;
