-- // RMP-12132 Replace Organisation concept with Workspace concept
-- Migration SQL that makes the change goes here.

ALTER TABLE organization RENAME TO workspace;

ALTER TABLE user_org_permissions RENAME TO user_workspace_permissions;

ALTER TABLE stack RENAME COLUMN organization_id TO workspace_id;

ALTER TABLE recipe RENAME COLUMN organization_id TO workspace_id;

ALTER TABLE blueprint RENAME COLUMN organization_id TO workspace_id;

ALTER TABLE credential RENAME COLUMN organization_id TO workspace_id;

ALTER TABLE managementpack RENAME COLUMN organization_id TO workspace_id;

ALTER TABLE ldapconfig RENAME COLUMN organization_id TO workspace_id;

ALTER TABLE rdsconfig RENAME COLUMN organization_id TO workspace_id;

ALTER TABLE imagecatalog RENAME COLUMN organization_id TO workspace_id;

ALTER TABLE proxyconfig RENAME COLUMN organization_id TO workspace_id;

ALTER TABLE cluster RENAME COLUMN organization_id TO workspace_id;

ALTER TABLE template RENAME COLUMN organization_id TO workspace_id;

ALTER TABLE clustertemplate RENAME COLUMN organization_id TO workspace_id;

ALTER TABLE constrainttemplate RENAME COLUMN organization_id TO workspace_id;

ALTER TABLE network RENAME COLUMN organization_id TO workspace_id;

ALTER TABLE securitygroup RENAME COLUMN organization_id TO workspace_id;

ALTER TABLE topology RENAME COLUMN organization_id TO workspace_id;

ALTER TABLE flexsubscription RENAME COLUMN organization_id TO workspace_id;

ALTER TABLE smartsensesubscription RENAME COLUMN organization_id TO workspace_id;

ALTER TABLE filesystem RENAME COLUMN organization_id TO workspace_id;

ALTER TABLE structuredevent RENAME COLUMN organization_id TO workspace_id;

ALTER TABLE user_workspace_permissions RENAME COLUMN organization_id TO workspace_id;

ALTER SEQUENCE IF EXISTS organization_id_seq RENAME TO workspace_id_seq;

ALTER SEQUENCE IF EXISTS user_org_permissions_id_seq RENAME TO user_workspace_permissions_id_seq;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER SEQUENCE IF EXISTS workspace_id_seq RENAME TO organization_id_seq;

ALTER SEQUENCE IF EXISTS user_workspace_permissions_id_seq RENAME TO user_org_permissions_id_seq;

ALTER TABLE stack RENAME COLUMN workspace_id TO organization_id;

ALTER TABLE recipe RENAME COLUMN workspace_id TO organization_id;

ALTER TABLE blueprint RENAME COLUMN workspace_id TO organization_id;

ALTER TABLE credential RENAME COLUMN workspace_id TO organization_id;

ALTER TABLE managementpack RENAME COLUMN workspace_id TO organization_id;

ALTER TABLE ldapconfig RENAME COLUMN workspace_id TO organization_id;

ALTER TABLE rdsconfig RENAME COLUMN workspace_id TO organization_id;

ALTER TABLE imagecatalog RENAME COLUMN workspace_id TO organization_id;

ALTER TABLE proxyconfig RENAME COLUMN workspace_id TO organization_id;

ALTER TABLE cluster RENAME COLUMN workspace_id TO organization_id;

ALTER TABLE template RENAME COLUMN workspace_id TO organization_id;

ALTER TABLE clustertemplate RENAME COLUMN workspace_id TO organization_id;

ALTER TABLE constrainttemplate RENAME COLUMN workspace_id TO organization_id;

ALTER TABLE network RENAME COLUMN workspace_id TO organization_id;

ALTER TABLE securitygroup RENAME COLUMN workspace_id TO organization_id;

ALTER TABLE topology RENAME COLUMN workspace_id TO organization_id;

ALTER TABLE flexsubscription RENAME COLUMN workspace_id TO organization_id;

ALTER TABLE smartsensesubscription RENAME COLUMN workspace_id TO organization_id;

ALTER TABLE filesystem RENAME COLUMN workspace_id TO organization_id;

ALTER TABLE structuredevent RENAME COLUMN workspace_id TO organization_id;

ALTER TABLE user_workspace_permissions RENAME COLUMN workspace_id TO organization_id;

ALTER TABLE workspace RENAME TO organization;

ALTER TABLE user_workspace_permissions RENAME TO user_org_permissions;