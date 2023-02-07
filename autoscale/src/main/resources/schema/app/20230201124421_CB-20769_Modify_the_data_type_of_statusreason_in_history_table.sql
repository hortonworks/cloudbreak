-- // CB-20769 Modify the data type of statusreason in history table
-- Migration SQL that makes the change goes here.

ALTER TABLE history ALTER COLUMN status_reason TYPE TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE history ALTER COLUMN status_reason TYPE varchar(255);
