-- // CB-6897 Improve InstanceMetadata with instance lifecycle
-- Migration SQL that makes the change goes here.

ALTER TABLE instancemetadata ADD COLUMN IF NOT EXISTS lifecycle VARCHAR(255) DEFAULT 'NORMAL';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE instancemetadata DROP COLUMN IF EXISTS lifecycle;
