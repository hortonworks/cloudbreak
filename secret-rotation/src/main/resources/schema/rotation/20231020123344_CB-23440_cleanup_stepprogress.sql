-- // CB-23440 refactor step progress
-- Migration SQL that makes the change goes here.

DROP INDEX IF EXISTS idx_rotationprogress_secrettype_step_executiontype;
ALTER TABLE secretrotationstepprogress DROP COLUMN IF EXISTS executiontype;
ALTER TABLE secretrotationstepprogress DROP COLUMN IF EXISTS created;
ALTER TABLE secretrotationstepprogress DROP COLUMN IF EXISTS finished;

-- //@UNDO
-- SQL to undo the change goes here.
