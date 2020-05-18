-- // CB-6236 enabling Ranger Raz.
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS ranger_raz_enabled boolean;
ALTER TABLE sdxcluster ALTER ranger_raz_enabled SET DEFAULT false;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxcluster DROP COLUMN IF EXISTS ranger_raz_enabled;

