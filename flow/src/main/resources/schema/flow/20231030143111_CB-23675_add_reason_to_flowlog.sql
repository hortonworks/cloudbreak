-- // CB-23675 Add reason to flowlog table
-- Migration SQL that makes the change goes here.

ALTER TABLE IF EXISTS flowlog ADD COLUMN IF NOT EXISTS reason text;

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE IF EXISTS flowlog DROP COLUMN IF EXISTS reason;
