-- // CLOUD-41876 private instance id
-- Migration SQL that makes the change goes here.

ALTER TABLE instancemetadata ADD COLUMN privateid BIGINT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE instancemetadata DROP COLUMN privateid;