-- // CB-4425 Experimental features step 3 - removing unused columns
-- Migration SQL that makes the change goes here.

ALTER TABLE environment DROP COLUMN if EXISTS tunnel;
ALTER TABLE environment DROP COLUMN if EXISTS idbroker_mapping_source;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS idbroker_mapping_source VARCHAR(10);
UPDATE environment set idbroker_mapping_source='MOCK';

ALTER TABLE environment ADD COLUMN IF NOT EXISTS tunnel varchar(255) NOT NULL DEFAULT 'DIRECT';
UPDATE environment SET tunnel = 'DIRECT';