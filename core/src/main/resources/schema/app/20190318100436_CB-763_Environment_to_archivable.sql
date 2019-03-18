-- // CB-763 Environment to archivable
-- Migration SQL that makes the change goes here.
ALTER TABLE IF EXISTS environment ADD COLUMN archived boolean DEFAULT FALSE;


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE IF EXISTS environment DROP COLUMN archived;

