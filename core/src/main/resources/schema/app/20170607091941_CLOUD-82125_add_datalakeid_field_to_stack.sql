-- // CLOUD-82125 add datalakeid field to stack
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN datalakeid bigint;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN datalakeid;

