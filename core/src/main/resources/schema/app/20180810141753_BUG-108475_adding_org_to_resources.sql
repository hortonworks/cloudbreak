-- // BUG-108475
-- Migration SQL that makes the change goes here.

-- cluster resource
ALTER TABLE cluster
    ADD organization_id int8,
    ADD CONSTRAINT clustername_in_org_unique UNIQUE (name, organization_id),
    ADD CONSTRAINT fk_cluster_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

UPDATE cluster
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.userid AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

CREATE UNIQUE INDEX IF NOT EXISTS       cluster_id_idx                        ON cluster (id);
CREATE INDEX IF NOT EXISTS              cluster_name_idx                      ON cluster (name);
CREATE INDEX IF NOT EXISTS              cluster_org_id_idx                    ON cluster (organization_id);

-- template resource
ALTER TABLE template
    ADD organization_id int8,
    ADD CONSTRAINT templatename_in_org_unique UNIQUE (name, organization_id),
    ADD CONSTRAINT fk_template_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

UPDATE template
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.userid AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

CREATE UNIQUE INDEX IF NOT EXISTS       template_id_idx                        ON template (id);
CREATE INDEX IF NOT EXISTS              template_name_idx                      ON template (name);
CREATE INDEX IF NOT EXISTS              template_org_id_idx                    ON template (organization_id);

-- clustertemplate resource
ALTER TABLE clustertemplate
    ADD organization_id int8,
    ADD CONSTRAINT clustertemplatename_in_org_unique UNIQUE (name, organization_id),
    ADD CONSTRAINT fk_clustertemplate_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

UPDATE clustertemplate
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.userid AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

CREATE UNIQUE INDEX IF NOT EXISTS       clustertemplate_id_idx                        ON clustertemplate (id);
CREATE INDEX IF NOT EXISTS              clustertemplate_name_idx                      ON clustertemplate (name);
CREATE INDEX IF NOT EXISTS              clustertemplate_org_id_idx                    ON clustertemplate (organization_id);

-- constrainttemplate resource
ALTER TABLE constrainttemplate
    ADD organization_id int8,
    ADD CONSTRAINT constrainttemplatename_in_org_unique UNIQUE (name, organization_id),
    ADD CONSTRAINT fk_constrainttemplate_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

UPDATE constrainttemplate
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.userid AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

CREATE UNIQUE INDEX IF NOT EXISTS       constrainttemplate_id_idx                        ON constrainttemplate (id);
CREATE INDEX IF NOT EXISTS              constrainttemplate_name_idx                      ON constrainttemplate (name);
CREATE INDEX IF NOT EXISTS              constrainttemplate_org_id_idx                    ON constrainttemplate (organization_id);

-- network resource
ALTER TABLE network
    ADD organization_id int8,
    ADD CONSTRAINT networkname_in_org_unique UNIQUE (name, organization_id),
    ADD CONSTRAINT fk_network_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

UPDATE network
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.userid AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

CREATE UNIQUE INDEX IF NOT EXISTS       network_id_idx                        ON network (id);
CREATE INDEX IF NOT EXISTS              network_name_idx                      ON network (name);
CREATE INDEX IF NOT EXISTS              network_org_id_idx                    ON network (organization_id);

-- securitygroup resource
ALTER TABLE securitygroup
    ADD organization_id int8,
    ADD CONSTRAINT securitygroupname_in_org_unique UNIQUE (name, organization_id),
    ADD CONSTRAINT fk_securitygroup_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

UPDATE securitygroup
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.userid AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

CREATE UNIQUE INDEX IF NOT EXISTS       securitygroup_id_idx                        ON securitygroup (id);
CREATE INDEX IF NOT EXISTS              securitygroup_name_idx                      ON securitygroup (name);
CREATE INDEX IF NOT EXISTS              securitygroup_org_id_idx                    ON securitygroup (organization_id);

-- topology resource
ALTER TABLE topology
    ADD organization_id int8,
    ADD CONSTRAINT topologyname_in_org_unique UNIQUE (name, organization_id),
    ADD CONSTRAINT fk_topology_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

UPDATE topology
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.userid AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

CREATE UNIQUE INDEX IF NOT EXISTS       topology_id_idx                        ON topology (id);
CREATE INDEX IF NOT EXISTS              topology_name_idx                      ON topology (name);
CREATE INDEX IF NOT EXISTS              topology_org_id_idx                    ON topology (organization_id);

-- flexsubscription resource
ALTER TABLE flexsubscription
    ADD organization_id int8,
    ADD CONSTRAINT flexsubscriptionname_in_org_unique UNIQUE (name, organization_id),
    ADD CONSTRAINT fk_flexsubscription_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

UPDATE flexsubscription
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.userid AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

CREATE UNIQUE INDEX IF NOT EXISTS       flexsubscription_id_idx                        ON flexsubscription (id);
CREATE INDEX IF NOT EXISTS              flexsubscription_name_idx                      ON flexsubscription (name);
CREATE INDEX IF NOT EXISTS              flexsubscription_org_id_idx                    ON flexsubscription (organization_id);

-- smartsense resource
ALTER TABLE smartsensesubscription
    ADD organization_id int8,
    ADD CONSTRAINT smartsensesubscriptionsubscriptionid_in_org_unique UNIQUE (subscriptionid, organization_id),
    ADD CONSTRAINT fk_smartsensesubscription_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

UPDATE smartsensesubscription
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.userid AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

CREATE UNIQUE INDEX IF NOT EXISTS       smartsensesubscription_id_idx                        ON smartsensesubscription (id);
CREATE INDEX IF NOT EXISTS              smartsensesubscription_subscriptionid_idx            ON smartsensesubscription (subscriptionid);
CREATE INDEX IF NOT EXISTS              smartsensesubscription_org_id_idx                    ON smartsensesubscription (organization_id);

-- filesystem resource
ALTER TABLE filesystem
    ADD organization_id int8,
    ADD CONSTRAINT filesystemname_in_org_unique UNIQUE (name, organization_id),
    ADD CONSTRAINT fk_filesystem_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

UPDATE filesystem
SET organization_id = subquery.org_id
FROM (SELECT userprofile.owner AS up_owner, users.userid AS u_email, user_org_permissions.organization_id AS org_id
      FROM userprofile
      INNER JOIN users ON userprofile.username=users.userid
      INNER JOIN user_org_permissions ON users.id=user_org_permissions.user_id) AS subquery
WHERE owner=up_owner;

CREATE UNIQUE INDEX IF NOT EXISTS       filesystem_id_idx                        ON filesystem (id);
CREATE INDEX IF NOT EXISTS              filesystem_name_idx                      ON filesystem (name);
CREATE INDEX IF NOT EXISTS              filesystem_org_id_idx                    ON filesystem (organization_id);

-- userprofile resource
ALTER TABLE userprofile
    ADD user_id int8,
    ADD CONSTRAINT fk_userprofile_user FOREIGN KEY (user_id) REFERENCES users(id);

UPDATE userprofile
    SET user_id = users.id
    FROM users
    WHERE users.userid = userprofile.username;

CREATE UNIQUE INDEX IF NOT EXISTS       userprofile_id_idx                        ON userprofile (id);
CREATE INDEX IF NOT EXISTS              userprofile_user_id_idx                   ON userprofile (user_id);




-- //@UNDO
-- SQL to undo the change goes here.

-- cluster resource
DROP INDEX IF EXISTS       cluster_id_idx;
DROP INDEX IF EXISTS       cluster_name_idx;
DROP INDEX IF EXISTS       cluster_org_id_idx;

ALTER TABLE cluster
    DROP CONSTRAINT IF EXISTS clustername_in_org_unique,
    DROP CONSTRAINT IF EXISTS fk_cluster_organization,
    DROP COLUMN IF EXISTS organization_id;

-- template resource
DROP INDEX IF EXISTS       template_id_idx;
DROP INDEX IF EXISTS       template_name_idx;
DROP INDEX IF EXISTS       template_org_id_idx;

ALTER TABLE template
    DROP CONSTRAINT IF EXISTS templatename_in_org_unique,
    DROP CONSTRAINT IF EXISTS fk_template_organization,
    DROP COLUMN IF EXISTS organization_id;

-- clustertemplate resource
DROP INDEX IF EXISTS       clustertemplate_id_idx;
DROP INDEX IF EXISTS       clustertemplate_name_idx;
DROP INDEX IF EXISTS       clustertemplate_org_id_idx;

ALTER TABLE clustertemplate
    DROP CONSTRAINT IF EXISTS clustertemplatename_in_org_unique,
    DROP CONSTRAINT IF EXISTS fk_clustertemplate_organization,
    DROP COLUMN IF EXISTS organization_id;

-- constrainttemplate resource
DROP INDEX IF EXISTS       constrainttemplate_id_idx;
DROP INDEX IF EXISTS       constrainttemplate_name_idx;
DROP INDEX IF EXISTS       constrainttemplate_org_id_idx;

ALTER TABLE constrainttemplate
    DROP CONSTRAINT IF EXISTS constrainttemplatename_in_org_unique,
    DROP CONSTRAINT IF EXISTS fk_constrainttemplate_organization,
    DROP COLUMN IF EXISTS organization_id;

-- network resource
DROP INDEX IF EXISTS       network_id_idx;
DROP INDEX IF EXISTS       network_name_idx;
DROP INDEX IF EXISTS       network_org_id_idx;

ALTER TABLE network
    DROP CONSTRAINT IF EXISTS networkname_in_org_unique,
    DROP CONSTRAINT IF EXISTS fk_network_organization,
    DROP COLUMN IF EXISTS organization_id;

-- securitygroup resource
DROP INDEX IF EXISTS       securitygroup_id_idx;
DROP INDEX IF EXISTS       securitygroup_name_idx;
DROP INDEX IF EXISTS       securitygroup_org_id_idx;

ALTER TABLE securitygroup
    DROP CONSTRAINT IF EXISTS securitygroupname_in_org_unique,
    DROP CONSTRAINT IF EXISTS fk_securitygroup_organization,
    DROP COLUMN IF EXISTS organization_id;

-- topology resource
DROP INDEX IF EXISTS       topology_id_idx;
DROP INDEX IF EXISTS       topology_name_idx;
DROP INDEX IF EXISTS       topology_org_id_idx;

ALTER TABLE topology
    DROP CONSTRAINT IF EXISTS topologyname_in_org_unique,
    DROP CONSTRAINT IF EXISTS fk_topology_organization,
    DROP COLUMN IF EXISTS organization_id;

-- flexsubscription resource
DROP INDEX IF EXISTS       flexsubscription_id_idx;
DROP INDEX IF EXISTS       flexsubscription_name_idx;
DROP INDEX IF EXISTS       flexsubscription_org_id_idx;

ALTER TABLE flexsubscription
    DROP CONSTRAINT IF EXISTS flexsubscriptionname_in_org_unique,
    DROP CONSTRAINT IF EXISTS fk_flexsubscription_organization,
    DROP COLUMN IF EXISTS organization_id;

-- smartsense resource
DROP INDEX IF EXISTS       smartsensesubscription_id_idx;
DROP INDEX IF EXISTS       smartsensesubscription_subscriptionid_idx;
DROP INDEX IF EXISTS       smartsensesubscription_org_id_idx;

ALTER TABLE smartsensesubscription
    DROP CONSTRAINT IF EXISTS smartsensesubscriptionsubscriptionid_in_org_unique,
    DROP CONSTRAINT IF EXISTS fk_smartsensesubscription_organization,
    DROP COLUMN IF EXISTS organization_id;

-- filesystem resource
DROP INDEX IF EXISTS       filesystem_id_idx;
DROP INDEX IF EXISTS       filesystem_name_idx;
DROP INDEX IF EXISTS       filesystem_org_id_idx;

ALTER TABLE filesystem
    DROP CONSTRAINT IF EXISTS filesystemname_in_org_unique,
    DROP CONSTRAINT IF EXISTS fk_filesystem_organization,
    DROP COLUMN IF EXISTS organization_id;

-- userprofile resource
DROP INDEX IF EXISTS       userprofile_id_idx;
DROP INDEX IF EXISTS       userprofile_user_id_idx;

ALTER TABLE userprofile
    DROP CONSTRAINT IF EXISTS fk_userprofile_user,
    DROP COLUMN IF EXISTS user_id;
