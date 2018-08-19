-- // BUG-108475
-- Migration SQL that makes the change goes here.

ALTER TABLE structuredevent
    ADD users_user_id int8,
    ADD CONSTRAINT fk_structuredevent_users FOREIGN KEY (users_user_id) REFERENCES users(id);

UPDATE structuredevent
SET users_user_id = subquery.u_user_id
FROM (SELECT userprofile.owner AS up_owner, users.id AS u_user_id, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE userid=up_owner;

UPDATE stack
SET createdby = subquery.u_user_id
FROM (SELECT userprofile.owner AS up_owner, users.id AS u_user_id, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

CREATE INDEX IF NOT EXISTS structuredevent_users_user_id_idx ON structuredevent (users_user_id);
CREATE INDEX IF NOT EXISTS structuredevent_organization_id_idx ON structuredevent (organization_id);
CREATE INDEX IF NOT EXISTS stack_createdby_idx ON stack (createdby);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS structuredevent_users_user_id_idx;
DROP INDEX IF EXISTS structuredevent_organization_id_idx;
DROP INDEX IF EXISTS stack_createdby_idx;

ALTER TABLE structuredevent DROP CONSTRAINT IF EXISTS fk_structuredevent_users;
ALTER TABLE structuredevent DROP COLUMN IF EXISTS users_user_id;
