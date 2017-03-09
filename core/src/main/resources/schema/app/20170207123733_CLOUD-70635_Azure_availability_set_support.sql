-- // CLOUD-70635 Azure availability set support
-- Migration SQL that makes the change goes here.

ALTER TABLE instancegroup ADD COLUMN attributes TEXT;
ALTER TABLE instancemetadata
   RENAME COLUMN hypervisor TO localityindicator;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE instancegroup DROP COLUMN attributes;
ALTER TABLE instancemetadata
   RENAME COLUMN localityindicator TO hypervisor;


