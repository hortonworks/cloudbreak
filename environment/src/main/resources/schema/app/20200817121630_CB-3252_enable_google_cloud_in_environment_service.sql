-- // CB-3252 enable google cloud in environment service
-- Migration SQL that makes the change goes here.


ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS networkId varchar(255);
ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS sharedProjectId varchar(255);
ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS noPublicIp bool DEFAULT false;
ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS noFirewallRules bool DEFAULT false;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_network DROP COLUMN IF EXISTS networkId;
ALTER TABLE environment_network DROP COLUMN IF EXISTS sharedProjectId;
ALTER TABLE environment_network DROP COLUMN IF EXISTS noFirewallRules;
ALTER TABLE environment_network DROP COLUMN IF EXISTS noPublicIp;



