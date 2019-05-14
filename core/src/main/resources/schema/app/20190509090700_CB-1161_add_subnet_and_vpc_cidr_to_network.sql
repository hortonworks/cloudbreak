-- // CB-1161 add subnet and vpc cidr to network
-- Migration SQL that makes the change goes here.

ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS subnets TEXT;
ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS networkCidr varchar(255);
ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS registrationType varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_network DROP COLUMN IF EXISTS subnets;
ALTER TABLE environment_network DROP COLUMN IF EXISTS networkCidr;
ALTER TABLE environment_network DROP COLUMN IF EXISTS registrationType;