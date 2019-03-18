-- // CB-763 Environment to archivable
-- Migration SQL that makes the change goes here.
ALTER TABLE IF EXISTS environment ADD COLUMN archived boolean DEFAULT FALSE;
ALTER TABLE IF EXISTS environment ADD COLUMN deletionTimestamp BIGINT;
ALTER TABLE IF EXISTS environment DROP CONSTRAINT uk_environment_workspace_name;
ALTER TABLE IF EXISTS environment ADD CONSTRAINT uk_environment_deletiondate_workspace_name UNIQUE (name, deletionTimestamp, workspace_id)


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP CONSTRAINT uk_environment_deletiondate_workspace_name;
ALTER TABLE environment ADD CONSTRAINT uk_environment_workspace_name UNIQUE (workspace_id, name);
ALTER TABLE IF EXISTS environment DROP COLUMN IF EXISTS deletionTimestamp;
ALTER TABLE IF EXISTS environment DROP COLUMN IF EXISTS archived;

