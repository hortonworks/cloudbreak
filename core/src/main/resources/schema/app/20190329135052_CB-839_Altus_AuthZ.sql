-- // CB-839 Connect Workspace AuthZ to IAM/UMS
-- Migration SQL that makes the change goes here.

CREATE TABLE user_workspace_permissions_bkp AS SELECT id, user_id, workspace_id, permissions FROM user_workspace_permissions;
DROP TABLE user_workspace_permissions;
ALTER TABLE users DROP COLUMN tenant_permissions, DROP COLUMN cloudbreak_permissions, ADD COLUMN crn TEXT;
ALTER TABLE workspace ADD COLUMN resourcecrn TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

CREATE TABLE user_workspace_permissions AS SELECT id, user_id, workspace_id, permissions FROM user_workspace_permissions_bkp;
DROP TABLE user_workspace_permissions_bkp;
ALTER TABLE users ADD COLUMN tenant_permissions TEXT, ADD COLUMN cloudbreak_permissions TEXT, DROP COLUMN crn;
ALTER TABLE workspace DROP COLUMN resourcecrn;