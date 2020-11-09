-- // CB-9127 optimize cluster template load time
-- Migration SQL that makes the change goes here.

DELETE FROM clustertemplate WHERE status='DEFAULT';
ALTER TABLE clustertemplate ADD COLUMN IF NOT EXISTS clouderaRuntimeVersion VARCHAR(255);

ALTER TABLE instancegroup ADD COLUMN IF NOT EXISTS initialNodeCount INTEGER;
ALTER TABLE instancegroup ALTER COLUMN initialNodeCount SET DEFAULT 0;
UPDATE instancegroup SET initialNodeCount=(SELECT COUNT(*) FROM instancemetadata WHERE instancegroup_id = instancegroup.id);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE clustertemplate DROP COLUMN IF EXISTS clouderaRuntimeVersion;
ALTER TABLE instancegroup DROP COLUMN IF EXISTS initialNodeCount;

