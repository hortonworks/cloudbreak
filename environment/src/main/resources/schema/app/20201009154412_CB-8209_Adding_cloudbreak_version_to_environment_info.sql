-- // CB-8209 Adding cloudbreak version to environment info
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS environment_service_version VARCHAR(128) DEFAULT 'No Info';
UPDATE environment SET environment_service_version = 'No Info' WHERE environment_service_version IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS environment_service_version;
