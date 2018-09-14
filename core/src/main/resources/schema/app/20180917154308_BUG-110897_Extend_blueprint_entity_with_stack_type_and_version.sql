-- // BUG-110897 Extend bleuprint entity with stack type and version
-- Migration SQL that makes the change goes here.

ALTER TABLE blueprint ADD COLUMN IF NOT EXISTS stacktype TEXT;
ALTER TABLE blueprint ADD COLUMN IF NOT EXISTS stackversion TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE blueprint DROP COLUMN IF EXISTS stacktype;
ALTER TABLE blueprint DROP COLUMN IF EXISTS stackversion;
