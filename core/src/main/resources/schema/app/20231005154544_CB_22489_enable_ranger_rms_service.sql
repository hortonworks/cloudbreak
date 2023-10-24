-- // CB_22489_enable_ranger_rms_service
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS ranger_rms_enabled boolean;
ALTER TABLE cluster ALTER ranger_rms_enabled SET DEFAULT false;

UPDATE cluster SET ranger_rms_enabled = FALSE
               WHERE ranger_rms_enabled IS NULL;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS ranger_rms_enabled;

