-- // CB-32106 Implement instanceType fallback for aws native
-- Migration SQL that makes the change goes here.

ALTER TABLE template ADD COLUMN IF NOT EXISTS fallbackInstanceTypes TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE template DROP COLUMN IF EXISTS fallbackInstanceTypes;
