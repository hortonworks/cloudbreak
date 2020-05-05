-- // CB-6897 Improve InstanceMetadata with instance lifecycle
-- Migration SQL that makes the change goes here.

ALTER TABLE instancemetadata ADD COLUMN IF NOT EXISTS lifecycle VARCHAR(255);

ALTER TABLE instancemetadata ALTER lifecycle SET DEFAULT 'NORMAL';

UPDATE instancemetadata
  SET lifecycle = 'NORMAL'
  WHERE lifecycle IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE instancemetadata DROP COLUMN IF EXISTS lifecycle;
