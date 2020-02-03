-- // CB-5067 Add externaldatabase field to the stack table
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD IF NOT EXISTS externaldatabasecreationtype VARCHAR(10) DEFAULT 'NONE';
UPDATE stack SET externaldatabasecreationtype = 'NONE' WHERE externaldatabasecreationtype IS NULL;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS externaldatabasecreationtype;
