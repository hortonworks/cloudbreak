-- // CB-2405 add resourcecrn to structured event
-- Migration SQL that makes the change goes here.

ALTER TABLE structuredevent ADD COLUMN IF NOT EXISTS resourceCrn VARCHAR(255);

ALTER TABLE recipe RENAME COLUMN crn TO resourceCrn;
ALTER TABLE imagecatalog RENAME COLUMN crn TO resourceCrn;
ALTER TABLE blueprint RENAME COLUMN crn TO resourceCrn;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE structuredevent DROP COLUMN IF EXISTS resourceCrn;

ALTER TABLE recipe RENAME COLUMN resourceCrn TO crn;
ALTER TABLE imagecatalog RENAME COLUMN resourceCrn TO crn;
ALTER TABLE blueprint RENAME COLUMN resourceCrn TO crn;
