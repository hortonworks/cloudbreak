-- // CB-21065 Add hits for multi AZ placement.
-- Migration SQL that makes the change goes here.

ALTER TABLE instancegroup ADD COLUMN IF NOT EXISTS hints VARCHAR(10) NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE instancegroup DROP COLUMN IF EXISTS hints;