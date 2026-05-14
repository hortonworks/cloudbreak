-- // CB-32683 Change global default blueprint without workspace
-- Migration SQL that makes the change goes here.

ALTER TABLE blueprint
DROP
CONSTRAINT blueprintname_in_org_unique;

CREATE UNIQUE INDEX IF NOT EXISTS blueprint_name_workspace_idx
    ON blueprint (name, workspace_id)
    WHERE workspace_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS blueprint_name_null_workspace_idx
    ON blueprint (name)
    WHERE workspace_id IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS blueprint_name_workspace_idx;

DROP INDEX IF EXISTS blueprint_name_null_workspace_idx;

ALTER TABLE blueprint
    ADD CONSTRAINT blueprintname_in_org_unique UNIQUE (name, workspace_id);