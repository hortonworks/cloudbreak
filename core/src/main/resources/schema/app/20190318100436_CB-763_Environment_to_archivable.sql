-- // CB-763 Environment to archivable
-- Migration SQL that makes the change goes here.
ALTER TABLE IF EXISTS environment
    ADD COLUMN IF NOT EXISTS archived boolean DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deletionTimestamp BIGINT DEFAULT -1,
    DROP CONSTRAINT uk_environment_workspace_name,
    ADD CONSTRAINT uk_environment_deletiondate_workspace UNIQUE (name, deletionTimestamp, workspace_id);

-- //@UNDO
-- SQL to undo the change goes here.
UPDATE cluster SET environment_id=null WHERE environment_id IN (SELECT id FROM environment WHERE archived=true);
UPDATE stack SET environment_id=null WHERE environment_id IN (SELECT id FROM environment WHERE archived=true);
DELETE FROM environment WHERE archived=true;
ALTER TABLE environment
    DROP CONSTRAINT IF EXISTS uk_environment_deletiondate_workspace,
    ADD CONSTRAINT uk_environment_workspace_name UNIQUE (workspace_id, name),
    DROP COLUMN IF EXISTS deletionTimestamp,
    DROP COLUMN IF EXISTS archived;

