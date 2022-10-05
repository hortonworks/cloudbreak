-- // CB-18654 Add Flow Endtime to flowlog
-- Migration SQL that makes the change goes here.

ALTER TABLE IF EXISTS flowlog ADD COLUMN IF NOT EXISTS endtime int8;

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE IF EXISTS flowlog DROP COLUMN IF EXISTS endtime;