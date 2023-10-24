-- // CB_22489_enable_ranger_rms_service
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS ranger_rms_enabled boolean;
ALTER TABLE sdxcluster ALTER ranger_rms_enabled SET DEFAULT FALSE;

UPDATE sdxcluster SET ranger_rms_enabled = FALSE
                  WHERE ranger_rms_enabled IS NULL;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxcluster DROP COLUMN IF EXISTS ranger_rms_enabled;
