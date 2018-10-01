-- // RMP-12337 - delete email sending from cb
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ALTER COLUMN emailneeded DROP NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.