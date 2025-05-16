-- // CB-26976 add privateid column to resource table
-- Migration SQL that makes the change goes here.

ALTER TABLE IF EXISTS resource ADD COLUMN IF NOT EXISTS privateid BIGINT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE IF EXISTS resource DROP COLUMN IF EXISTS privateid;
