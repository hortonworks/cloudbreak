-- // CLOUD-58085_ability_to_restart_flow
-- Migration SQL that makes the change goes here.

ALTER TABLE flowlog ADD COLUMN flowchainid varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE flowlog DROP COLUMN flowchainid;