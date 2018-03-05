-- // CLOUD-97533 refactor RDS config entity
-- Migration SQL that makes the change goes here.

ALTER TABLE rdsconfig DROP COLUMN IF EXISTS attributes;
ALTER TABLE rdsconfig RENAME COLUMN databasetype TO databaseengine;
ALTER TABLE rdsconfig ADD COLUMN connectiondriver varchar(255) DEFAULT 'org.postgresql.Driver';
ALTER TABLE rdsconfig RENAME COLUMN hdpversion TO stackversion;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE rdsconfig ADD COLUMN attributes TEXT;
ALTER TABLE rdsconfig RENAME COLUMN databaseengine TO databasetype;
ALTER TABLE rdsconfig DROP COLUMN IF EXISTS connectiondriver;
ALTER TABLE rdsconfig RENAME COLUMN stackversion TO hdpversion;
