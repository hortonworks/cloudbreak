-- // CB-1611-add-network-properties
-- Migration SQL that makes the change goes here.

ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS networkCidr varchar(255);
ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS registrationType  varchar(255) NOT NULL DEFAULT 'EXISTING';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_network DROP COLUMN IF EXISTS networkCidr;
ALTER TABLE environment_network DROP COLUMN IF EXISTS registrationType;