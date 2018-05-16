-- // “BUG-101027 parameterized root disk size”
-- Migration SQL that makes the change goes here.

ALTER TABLE template ADD COLUMN rootvolumesize INTEGER DEFAULT null;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE template DROP COLUMN rootvolumesize;
