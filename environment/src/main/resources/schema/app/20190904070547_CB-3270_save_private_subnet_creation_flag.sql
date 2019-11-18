-- // CB-3270_save_private_subnet_creation_flag
-- Migration SQL that makes the change goes here.

ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS privateSubnetCreation varchar(255) NOT NULL DEFAULT 'DISABLED';
ALTER TABLE environment_network DROP COLUMN IF EXISTS subnetids;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_network DROP COLUMN IF EXISTS privateSubnetCreation;
ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS subnetids TEXT;
