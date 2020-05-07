-- // CB-6764 introduce serviceEndpointCreation column in environment's network entity
-- Migration SQL that makes the change goes here.

ALTER TABLE environment_network ADD IF NOT EXISTS serviceEndpointCreation varchar(255);
ALTER TABLE environment_network ALTER COLUMN serviceEndpointCreation SET DEFAULT 'DISABLED';
UPDATE environment_network SET serviceEndpointCreation = 'DISABLED' WHERE serviceEndpointCreation IS NULL;
ALTER TABLE environment_network ALTER COLUMN serviceEndpointCreation SET NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_network DROP COLUMN IF EXISTS serviceEndpointCreation;
