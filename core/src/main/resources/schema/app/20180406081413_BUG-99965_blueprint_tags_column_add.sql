-- // BUG-99965 blueprint tags column add
-- Migration SQL that makes the change goes here.
ALTER TABLE blueprint ADD COLUMN IF NOT EXISTS tags VARCHAR DEFAULT '';


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE blueprint DROP COLUMN IF EXISTS tags;

