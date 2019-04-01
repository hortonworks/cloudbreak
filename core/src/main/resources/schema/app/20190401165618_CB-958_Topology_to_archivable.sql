-- // CB-958 Topology to archivable
-- Migration SQL that makes the change goes here.
ALTER TABLE IF EXISTS topology
    ADD COLUMN IF NOT EXISTS deletionTimestamp BIGINT DEFAULT -1,
    DROP CONSTRAINT topologyname_in_org_unique,
    ADD CONSTRAINT uk_topology_deletiondate_workspace UNIQUE (name, workspace_id, deletionTimestamp);

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE network SET topology_id=null WHERE topology_id IN (SELECT id FROM topology WHERE deleted=true);
UPDATE template SET topology_id=null WHERE topology_id IN (SELECT id FROM topology WHERE deleted=true);
DELETE FROM topology WHERE deleted=true;
ALTER TABLE topology
    DROP CONSTRAINT IF EXISTS uk_topology_deletiondate_workspace,
    ADD CONSTRAINT topologyname_in_org_unique UNIQUE (name, workspace_id),
    DROP COLUMN IF EXISTS deletionTimestamp;

