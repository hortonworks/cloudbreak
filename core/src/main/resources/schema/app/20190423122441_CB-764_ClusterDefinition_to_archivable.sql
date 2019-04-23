-- // CB-764 blueprint to archivable
-- Migration SQL that makes the change goes here.
ALTER TABLE IF EXISTS blueprint
    ADD COLUMN IF NOT EXISTS archived boolean DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deletionTimestamp BIGINT DEFAULT -1,
    DROP CONSTRAINT blueprintname_in_org_unique,
    ADD CONSTRAINT uk_blueprint_deletiondate_workspace UNIQUE (name, deletionTimestamp, workspace_id);
UPDATE blueprint SET archived=true, status='DEFAULT' WHERE status='DEFAULT_DELETED';

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE blueprint SET status='DEFAULT_DELETED',archived=false WHERE archived=true AND status='DEFAULT';
UPDATE cluster SET blueprint_id=null WHERE blueprint_id IN (SELECT id FROM blueprint WHERE archived=true);
DELETE FROM blueprint WHERE archived=true;
ALTER TABLE blueprint
    DROP CONSTRAINT IF EXISTS uk_blueprint_deletiondate_workspace,
    ADD CONSTRAINT blueprintname_in_org_unique UNIQUE (workspace_id, name),
    DROP COLUMN IF EXISTS deletionTimestamp,
    DROP COLUMN IF EXISTS archived;


