-- // secret rotation step progress
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS idx_rotationprogress_secrettype_step_executiontype
    ON secretrotationstepprogress
    USING btree (secrettype, secretrotationstep, executiontype);

ALTER TABLE secretrotationstepprogress ALTER COLUMN secrettype TYPE text;
ALTER TABLE secretrotationstepprogress ALTER COLUMN secretrotationstep TYPE text;


-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_rotationprogress_secrettype_step_executiontype;