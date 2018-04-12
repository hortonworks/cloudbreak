-- // BUG-98856 extend RDS config with connector jar URL
-- Migration SQL that makes the change goes here.

ALTER TABLE rdsconfig ADD COLUMN connectorjarurl varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE rdsconfig DROP COLUMN IF EXISTS connectorjarurl;
