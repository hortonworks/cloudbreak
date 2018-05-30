-- // persist cloud storage locations
-- Migration SQL that makes the change goes here.

ALTER TABLE filesystem ADD COLUMN IF NOT EXISTS locations TEXT DEFAULT '"locations":[]';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE filesystem DROP COLUMN IF EXISTS locations;
