-- // CB-10242 API changes for semi-private networks
-- Migration SQL that makes the change goes here.

ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS publicendpointaccessgateway CHARACTER VARYING(255);
ALTER TABLE environment_network ALTER COLUMN publicendpointaccessgateway SET DEFAULT FALSE;
UPDATE environment_network SET publicendpointaccessgateway = 'DISABLED' WHERE publicendpointaccessgateway IS NULL;

ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS endpointgatewaysubnetmetas TEXT;
ALTER TABLE environment_network ALTER COLUMN endpointgatewaysubnetmetas SET DEFAULT '{}';
UPDATE environment_network SET endpointgatewaysubnetmetas = '{}' WHERE endpointgatewaysubnetmetas IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_network DROP COLUMN IF EXISTS endpointgatewaysubnetmetas;
ALTER TABLE environment_network DROP COLUMN IF EXISTS publicendpointaccessgateway;
