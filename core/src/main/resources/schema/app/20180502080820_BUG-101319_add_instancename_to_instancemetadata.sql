-- // BUG-101319 add instancename to instancemetadata
-- Migration SQL that makes the change goes here.

ALTER TABLE instancemetadata ADD COLUMN instanceName VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE instancemetadata DROP COLUMN IF EXISTS instanceName;

