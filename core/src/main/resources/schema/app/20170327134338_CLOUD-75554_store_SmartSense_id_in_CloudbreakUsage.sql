-- // CLOUD-75554 store SmartSense id in CloudbreakUsage
-- Migration SQL that makes the change goes here.

ALTER TABLE cloudbreakusage ADD COLUMN smartsenseid character varying(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cloudbreakusage DROP COLUMN IF EXISTS smartsenseid;
