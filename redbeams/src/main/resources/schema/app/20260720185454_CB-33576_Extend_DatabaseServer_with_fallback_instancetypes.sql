-- // CB-33576 Extend DatabaseServer with fallback instancetypes
-- Migration SQL that makes the change goes here.

ALTER TABLE databaseserver ADD COLUMN IF NOT EXISTS fallbackinstancetypes TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE databaseserver DROP COLUMN IF EXISTS fallbackinstancetypes;
