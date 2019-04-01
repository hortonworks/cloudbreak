-- // CB-900 Network to archivable
-- Migration SQL that makes the change goes here.
ALTER TABLE IF EXISTS network
    ADD COLUMN IF NOT EXISTS archived boolean DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deletionTimestamp BIGINT DEFAULT -1,
    DROP CONSTRAINT networkname_in_org_unique,
    ADD CONSTRAINT uk_network_deletiondate_workspace UNIQUE (name, deletionTimestamp, workspace_id);

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE stack SET network_id=null WHERE network_id IN (SELECT id FROM network WHERE archived=true);
DELETE FROM network WHERE archived=true;
ALTER TABLE network
    DROP CONSTRAINT IF EXISTS uk_network_deletiondate_workspace,
    ADD CONSTRAINT networkname_in_org_unique UNIQUE (name, workspace_id),
    DROP COLUMN IF EXISTS deletionTimestamp,
    DROP COLUMN IF EXISTS archived;

