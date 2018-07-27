-- // BUG-107734_migrate_owner_into_organization
-- Migration SQL that makes the change goes here.

ALTER TABLE organization
ADD CONSTRAINT org_in_tenant_unique UNIQUE (name, tenant_id);

INSERT INTO tenant (id, name)
VALUES (0, 'Hortonworks');

INSERT INTO users (email, company, tenant_id, tenant_permissions)
SELECT DISTINCT username, 'Hortonworks', 0, '["ALL:read"]'
FROM userprofile
WHERE username IS NOT NULL;

INSERT INTO organization (name, tenant_id)
SELECT email, 0
FROM users;

INSERT INTO user_org_permissions (user_id, organization_id, permissions)
SELECT users.id, organization.id, '["ALL:read"]'
FROM users
INNER JOIN organization ON users.email=organization.name;

UPDATE stack
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.email AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.email
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

UPDATE recipe
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.email AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.email
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

UPDATE blueprint
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.email AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.email
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

UPDATE credential
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.email AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.email
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

UPDATE managementpack
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.email AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.email
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

UPDATE ldapconfig
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.email AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.email
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

UPDATE rdsconfig
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.email AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.email
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

UPDATE imagecatalog
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.email AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.email
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

UPDATE proxyconfig
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.email AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.email
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE stack
SET organization_id = null
WHERE organization_id is not null;

UPDATE recipe
SET organization_id = null
WHERE organization_id is not null;

UPDATE blueprint
SET organization_id = null
WHERE organization_id is not null;

UPDATE credential
SET organization_id = null
WHERE organization_id is not null;

UPDATE managementpack
SET organization_id = null
WHERE organization_id is not null;

UPDATE ldapconfig
SET organization_id = null
WHERE organization_id is not null;

UPDATE rdsconfig
SET organization_id = null
WHERE organization_id is not null;

UPDATE imagecatalog
SET organization_id = null
WHERE organization_id is not null;

UPDATE proxyconfig
SET organization_id = null
WHERE organization_id is not null;

TRUNCATE users, user_org_permissions;

DELETE FROM organization;

DELETE FROM TENANT;

ALTER TABLE organization
DROP CONSTRAINT IF EXISTS org_in_tenant_unique;