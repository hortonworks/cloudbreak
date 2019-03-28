-- // CB-839 Connect Workspace AuthZ to IAM/UMS
-- Migration SQL that makes the change goes here.

CREATE TABLE user_workspace_permissions_bkp AS SELECT id, user_id, workspace_id, permissions FROM user_workspace_permissions;
DROP TABLE user_workspace_permissions;
ALTER TABLE users DROP COLUMN tenant_permissions, DROP COLUMN cloudbreak_permissions;
ALTER TABLE workspace ADD COLUMN resourcecrn TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

CREATE TABLE IF NOT EXISTS user_workspace_permissions (
    id                      bigserial NOT NULL,
    permissions             TEXT NOT NULL,
    user_id                 int8 NOT NULL,
    workspace_id         int8 NOT NULL,

    CONSTRAINT              pk_user_to_workspace_id              PRIMARY KEY (id),
    CONSTRAINT              fk_user_to_workspace_user            FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT              fk_user_to_workspace_org             FOREIGN KEY (workspace_id) REFERENCES workspace(id)
);
INSERT INTO user_workspace_permissions (id, user_id, workspace_id, permissions) SELECT id, user_id, workspace_id, permissions FROM user_workspace_permissions_bkp;
DROP TABLE user_workspace_permissions_bkp;
ALTER TABLE users ADD COLUMN tenant_permissions TEXT, ADD COLUMN cloudbreak_permissions TEXT;
ALTER TABLE workspace DROP COLUMN resourcecrn;