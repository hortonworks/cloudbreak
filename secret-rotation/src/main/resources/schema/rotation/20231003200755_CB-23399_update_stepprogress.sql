-- // CB-23399 refactor step progress
-- Migration SQL that makes the change goes here.

ALTER TABLE secretrotationstepprogress ADD COLUMN IF NOT EXISTS currentExecutionType character varying(255);
ALTER TABLE secretrotationstepprogress ADD COLUMN IF NOT EXISTS status character varying(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE secretrotationstepprogress DROP COLUMN IF EXISTS currentExecutionType;
ALTER TABLE secretrotationstepprogress DROP COLUMN IF EXISTS status;