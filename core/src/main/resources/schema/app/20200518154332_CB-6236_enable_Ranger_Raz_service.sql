-- // CB-6236 enable Ranger Raz service.
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS ranger_raz_enabled boolean;
ALTER TABLE cluster ALTER ranger_raz_enabled SET DEFAULT false;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS ranger_raz_enabled;


