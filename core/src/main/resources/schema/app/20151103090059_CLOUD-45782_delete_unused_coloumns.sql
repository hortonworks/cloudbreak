-- // CLOUD-45782 delete unused coloumns
-- Migration SQL that makes the change goes here.

-- set privateId to unique value
UPDATE instancemetadata SET privateid = id WHERE privateid IS NULL;

ALTER TABLE instancemetadata ALTER COLUMN privateid SET NOT NULL;

ALTER TABLE instancemetadata DROP COLUMN dockersubnet;

ALTER TABLE instancemetadata DROP COLUMN containercount;


-- //@UNDO
-- SQL to undo the change goes here.


ALTER TABLE instancemetadata ADD COLUMN dockersubnet varchar (255);

ALTER TABLE instancemetadata ADD COLUMN containercount integer;

