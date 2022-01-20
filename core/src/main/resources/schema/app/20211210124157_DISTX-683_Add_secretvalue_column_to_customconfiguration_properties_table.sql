-- // DISTX-683 Add secretvalue column to customconfiguration_properties table
-- Migration SQL that makes the change goes here.
ALTER TABLE customconfigurations_properties ADD COLUMN IF NOT EXISTS secretvalue text;
ALTER TABLE customconfigurations_properties ALTER COLUMN secretvalue SET DEFAULT '{}';
UPDATE customconfigurations_properties SET secretvalue = '{}' WHERE secretvalue IS NULL;
ALTER TABLE customconfigurations_properties ALTER COLUMN secretvalue SET NOT NULL;
ALTER TABLE customconfigurations_properties ALTER COLUMN "value" DROP NOT NULL;


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE customconfigurations_properties DROP COLUMN IF EXISTS secretvalue;
UPDATE customconfigurations_properties SET "value" = '' WHERE "value" IS NULL;
ALTER TABLE customconfigurations_properties ALTER COLUMN "value" SET NOT NULL;

