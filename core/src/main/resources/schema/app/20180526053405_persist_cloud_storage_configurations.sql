-- // persist cloud storage configurations
-- Migration SQL that makes the change goes here.

ALTER TABLE filesystem ADD COLUMN IF NOT EXISTS configurations TEXT DEFAULT '{}';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE filesystem DROP COLUMN IF EXISTS configurations;

