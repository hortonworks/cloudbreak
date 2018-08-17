-- // BUG-108475 structued event catalog auth
-- Migration SQL that makes the change goes here.

ALTER TABLE structuredevent
    ADD organization_id int8,
    ADD CONSTRAINT fk_structuredevent_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

UPDATE structuredevent
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.userid AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE userid=up_owner;

ALTER TABLE structuredevent ALTER COLUMN userid DROP NOT NULL;
ALTER TABLE structuredevent ALTER COLUMN account DROP NOT NULL;
ALTER TABLE structuredevent DROP CONSTRAINT IF EXISTS uk_structuredevent_account_name;

-- //@UNDO
-- SQL to undo the change goes here.

UPDATE structuredevent
SET organization_id = NULL
WHERE organization_id IS NOT NULL;

ALTER TABLE structuredevent
    DROP CONSTRAINT IF EXISTS fk_structuredevent_organization,
    DROP COLUMN IF EXISTS organization_id;