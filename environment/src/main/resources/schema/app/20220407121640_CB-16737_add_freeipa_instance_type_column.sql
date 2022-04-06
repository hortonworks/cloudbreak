-- // CB-16737 alter environment table add freeipainstancetype column
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS freeipainstancetype VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS freeipainstancetype;