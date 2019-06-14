-- // CB-1617 freeipa creation is optional
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS createfreeipa boolean NOT NULL DEFAULT true;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS createFreeIpa;

