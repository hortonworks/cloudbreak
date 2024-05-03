-- // CB-25666 Add sidgeneration column to freeipa table
-- Migration SQL that makes the change goes here.

ALTER TABLE freeipa ADD COLUMN IF NOT EXISTS sidgeneration VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE freeipa DROP COLUMN IF EXISTS sidgeneration;
