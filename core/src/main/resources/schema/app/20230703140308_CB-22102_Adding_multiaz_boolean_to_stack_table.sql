-- // CB-22102 Adding multiaz boolean to stack table
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN IF NOT EXISTS multiaz bool DEFAULT false;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS multiaz;
