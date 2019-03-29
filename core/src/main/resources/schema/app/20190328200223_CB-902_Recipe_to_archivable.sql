-- // CB-902 Recipe to archivable
-- Migration SQL that makes the change goes here.
ALTER TABLE IF EXISTS recipe
    ADD COLUMN IF NOT EXISTS archived boolean DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deletionTimestamp BIGINT DEFAULT -1,
    DROP CONSTRAINT recipename_in_org_unique,
    ADD CONSTRAINT uk_recipe_deletiondate_workspace UNIQUE (name, deletionTimestamp, workspace_id);

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE hostgroup_recipe SET recipes_id=null WHERE recipes_id IN (SELECT id FROM recipe WHERE archived=true);
DELETE FROM recipe WHERE archived=true;
ALTER TABLE recipe
    DROP CONSTRAINT IF EXISTS uk_recipe_deletiondate_workspace,
    ADD CONSTRAINT recipename_in_org_unique UNIQUE (workspace_id, name),
    DROP COLUMN IF EXISTS deletionTimestamp,
    DROP COLUMN IF EXISTS archived;


