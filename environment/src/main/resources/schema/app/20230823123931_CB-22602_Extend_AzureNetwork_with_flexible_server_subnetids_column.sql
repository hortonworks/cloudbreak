-- // CB-22602 Extend AzureNetwork with flexible server subnetids column
-- Migration SQL that makes the change goes here.

ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS flexibleserversubnetids text DEFAULT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_network DROP COLUMN IF EXISTS flexibleserversubnetids;
