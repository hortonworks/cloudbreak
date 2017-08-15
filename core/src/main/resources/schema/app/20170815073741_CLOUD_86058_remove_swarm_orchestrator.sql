-- // CLOUD_86058_remove_swarm_orchestrator
-- Migration SQL that makes the change goes here.

ALTER TABLE stack DROP COLUMN relocatedocker;
ALTER TABLE cluster DROP COLUMN enableshipyard;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack ADD COLUMN relocatedocker boolean DEFAULT FALSE;
ALTER TABLE cluster ADD COLUMN enableshipyard boolean DEFAULT FALSE;
