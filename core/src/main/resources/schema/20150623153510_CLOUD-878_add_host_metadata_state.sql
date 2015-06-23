-- // CLOUD-878 add host metadata state
-- Migration SQL that makes the change goes here.

alter table hostmetadata ADD COLUMN hostmetadatastate VARCHAR (30) DEFAULT 'HEALTHY';

-- //@UNDO
-- SQL to undo the change goes here.

alter table hostmetadata DROP COLUMN hostmetadatastate;


