-- // CLOUD-53828 some stack related parameter
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN relocateDocker boolean DEFAULT FALSE;
ALTER TABLE cluster ADD COLUMN enableShipyard boolean DEFAULT FALSE;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN relocateDocker;
ALTER TABLE stack DROP COLUMN enableShipyard;

