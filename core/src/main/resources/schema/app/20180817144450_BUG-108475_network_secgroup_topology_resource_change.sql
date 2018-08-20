-- // network resource change
-- Migration SQL that makes the change goes here.

ALTER TABLE network ALTER COLUMN owner DROP NOT NULL;
ALTER TABLE network ALTER COLUMN account DROP NOT NULL;
ALTER TABLE network ALTER COLUMN publicinaccount DROP NOT NULL;
ALTER TABLE network DROP CONSTRAINT IF EXISTS uk_network_account_name;

ALTER TABLE securitygroup ALTER COLUMN owner DROP NOT NULL;
ALTER TABLE securitygroup ALTER COLUMN account DROP NOT NULL;
ALTER TABLE securitygroup ALTER COLUMN publicinaccount DROP NOT NULL;
ALTER TABLE securitygroup DROP CONSTRAINT IF EXISTS uk_securitygroupnameinaccount;

ALTER TABLE topology ALTER COLUMN owner DROP NOT NULL;
ALTER TABLE topology ALTER COLUMN account DROP NOT NULL;
ALTER TABLE topology DROP CONSTRAINT IF EXISTS uk_topology_name;

-- //@UNDO
-- SQL to undo the change goes here.


