-- // CB-26354 indexes for recipes
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS idx_recipe_name_archived_workspaceid ON recipe (name, archived, workspace_id);

CREATE INDEX IF NOT EXISTS idx_recipe_name_workspaceid ON recipe (name, workspace_id);

CREATE INDEX IF NOT EXISTS idx_hostgroup_recipe_recipes_id ON hostgroup_recipe (recipes_id);


-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_recipe_name_archived_workspaceid;

DROP INDEX IF EXISTS idx_recipe_name_workspaceid;

DROP INDEX IF EXISTS idx_hostgroup_recipe_recipes_id;

