-- // BUG-107734_migrate_owner_into_organization
-- Migration SQL that makes the change goes here.

-- because some data could be already in these tables and we want a fresh start for the migration
TRUNCATE users, user_org_permissions;
DELETE FROM organization;
--

ALTER TABLE users
DROP COLUMN IF EXISTS company;

ALTER TABLE tenant
ADD CONSTRAINT tenant_name_is_unique UNIQUE (name);

ALTER TABLE organization
ADD CONSTRAINT org_in_tenant_unique UNIQUE (name, tenant_id);

ALTER TABLE user_org_permissions
ADD CONSTRAINT user_org_pair_is_unique UNIQUE (user_id, organization_id);

ALTER TABLE users RENAME email TO userid;
ALTER TABLE users RENAME name TO username;

ALTER TABLE users
ADD CONSTRAINT users_userid_is_unique UNIQUE (userid);

INSERT INTO users (userid, tenant_id, tenant_permissions)
SELECT DISTINCT userprofile.username, tenant.id, '[]'
FROM userprofile, tenant
WHERE userprofile.username IS NOT null and tenant.name = 'DEFAULT';

INSERT INTO organization (name, tenant_id, description)
SELECT users.userid, tenant.id, 'Default organization for the user.'
FROM users, tenant
WHERE tenant.name = 'DEFAULT';

INSERT INTO user_org_permissions (user_id, organization_id, permissions)
SELECT users.id, organization.id, '["ORG:MANAGE","ALL:READ","ALL:WRITE"]'
FROM users
INNER JOIN organization ON users.userid=organization.name;

UPDATE stack
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.userid AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

UPDATE recipe
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.userid AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

UPDATE blueprint
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.userid AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

UPDATE credential
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.userid AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

UPDATE managementpack
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.userid AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

UPDATE ldapconfig
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.userid AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

UPDATE rdsconfig
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.userid AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

UPDATE imagecatalog
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.userid AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

UPDATE proxyconfig
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.userid AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
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

ALTER TABLE user_org_permissions
DROP CONSTRAINT IF EXISTS user_org_pair_is_unique;

ALTER TABLE users
DROP CONSTRAINT IF EXISTS users_userid_is_unique;

ALTER TABLE users RENAME username TO name;
ALTER TABLE users RENAME userid TO email;

ALTER TABLE organization
DROP CONSTRAINT IF EXISTS org_in_tenant_unique;

ALTER TABLE tenant
DROP CONSTRAINT IF EXISTS tenant_name_is_unique;

ALTER TABLE users
ADD COLUMN IF NOT EXISTS company VARCHAR (255);