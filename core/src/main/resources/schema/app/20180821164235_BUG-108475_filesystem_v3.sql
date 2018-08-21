-- // BUG-108475_filesystem_v3
-- Migration SQL that makes the change goes here.

ALTER TABLE filesystem ALTER COLUMN owner DROP NOT NULL;
ALTER TABLE filesystem ALTER COLUMN account DROP NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.


