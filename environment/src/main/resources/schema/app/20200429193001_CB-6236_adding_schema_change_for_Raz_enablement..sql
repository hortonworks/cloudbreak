-- // CB-6236 adding schema change for Raz enablement.
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS raz_enabled boolean NOT NULL DEFAULT false;
ALTER TABLE environment ADD COLUMN IF NOT EXISTS securitygroup_id_raz VARCHAR (255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS raz_enabled;
ALTER TABLE environment DROP COLUMN IF EXISTS securitygroup_id_raz;

