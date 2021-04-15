-- // CB-11907 Add freeipa image catalog and image id to environment
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS freeIpaImageCatalog character varying(255);
ALTER TABLE environment ADD COLUMN IF NOT EXISTS freeIpaImageId character varying(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS freeIpaImageId;
ALTER TABLE environment DROP COLUMN IF EXISTS freeIpaImageCatalog;

