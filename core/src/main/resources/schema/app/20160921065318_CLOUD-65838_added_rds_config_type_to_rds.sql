-- // CLOUD-65838 added rds config type to rds
-- Migration SQL that makes the change goes here.

ALTER TABLE rdsconfig ADD COLUMN attributes TEXT;
ALTER TABLE rdsconfig ADD COLUMN type varchar(255) DEFAULT 'HIVE';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE rdsconfig DROP COLUMN attributes;
ALTER TABLE rdsconfig DROP COLUMN type;
