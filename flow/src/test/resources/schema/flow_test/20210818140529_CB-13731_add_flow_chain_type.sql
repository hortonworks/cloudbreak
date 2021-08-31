-- // CB-13731 introduce flowChainType into the flowChainLog table
-- Migration SQL that makes the change goes here.

ALTER TABLE flowchainlog ADD COLUMN flowchaintype CHARACTER VARYING (255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE flowchainlog DROP COLUMN IF EXISTS flowchaintype;