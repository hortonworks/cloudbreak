-- // BUG-108475
-- Migration SQL that makes the change goes here.

ALTER TABLE recipe ALTER COLUMN owner DROP NOT NULL;
ALTER TABLE recipe ALTER COLUMN account DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS       tenant_id_idx                       ON tenant (id);
CREATE UNIQUE INDEX IF NOT EXISTS       tenant_name_idx                     ON tenant (name);

CREATE UNIQUE INDEX IF NOT EXISTS       organization_id_idx                 ON organization (id);
CREATE INDEX IF NOT EXISTS              organization_name_idx               ON organization (name);
CREATE INDEX IF NOT EXISTS              organization_tenant_id_idx          ON organization (tenant_id);
CREATE INDEX IF NOT EXISTS              organization_deletiontimestamp_idx  ON organization (deletiontimestamp);

CREATE UNIQUE INDEX IF NOT EXISTS       user_org_permissions_id_idx         ON user_org_permissions (id);
CREATE INDEX IF NOT EXISTS              user_org_permissions_user_id_idx    ON user_org_permissions (user_id);
CREATE INDEX IF NOT EXISTS              user_org_permissions_org_id_idx     ON user_org_permissions (organization_id);

CREATE UNIQUE INDEX IF NOT EXISTS       users_id_idx                        ON users (id);
CREATE UNIQUE INDEX IF NOT EXISTS       users_userid_idx                    ON users (userid);
CREATE INDEX IF NOT EXISTS              users_username_idx                  ON users (username);
CREATE INDEX IF NOT EXISTS              users_tenant_id_idx                 ON users (tenant_id);


CREATE UNIQUE INDEX IF NOT EXISTS       stack_id_idx                        ON stack (id);
CREATE INDEX IF NOT EXISTS              stack_name_idx                      ON stack (name);
CREATE INDEX IF NOT EXISTS              stack_org_id_idx                    ON stack (organization_id);

CREATE UNIQUE INDEX IF NOT EXISTS       recipe_id_idx                       ON recipe (id);
CREATE INDEX IF NOT EXISTS              recipe_name_idx                     ON recipe (name);
CREATE INDEX IF NOT EXISTS              recipe_org_id_idx                   ON recipe (organization_id);

CREATE UNIQUE INDEX IF NOT EXISTS       blueprint_id_idx                    ON blueprint (id);
CREATE INDEX IF NOT EXISTS              blueprint_name_idx                  ON blueprint (name);
CREATE INDEX IF NOT EXISTS              blueprint_org_id_idx                ON blueprint (organization_id);

CREATE UNIQUE INDEX IF NOT EXISTS       credential_id_idx                   ON credential (id);
CREATE INDEX IF NOT EXISTS              credential_name_idx                 ON credential (name);
CREATE INDEX IF NOT EXISTS              credential_org_id_idx               ON credential (organization_id);

CREATE UNIQUE INDEX IF NOT EXISTS       managementpack_id_idx               ON managementpack (id);
CREATE INDEX IF NOT EXISTS              managementpack_name_idx             ON managementpack (name);
CREATE INDEX IF NOT EXISTS              managementpack_org_id_idx           ON managementpack (organization_id);

CREATE UNIQUE INDEX IF NOT EXISTS       ldapconfig_id_idx                   ON ldapconfig (id);
CREATE INDEX IF NOT EXISTS              ldapconfig_name_idx                 ON ldapconfig (name);
CREATE INDEX IF NOT EXISTS              ldapconfig_org_id_idx               ON ldapconfig (organization_id);

CREATE UNIQUE INDEX IF NOT EXISTS       rdsconfig_id_idx                    ON rdsconfig (id);
CREATE INDEX IF NOT EXISTS              rdsconfig_name_idx                  ON rdsconfig (name);
CREATE INDEX IF NOT EXISTS              rdsconfig_org_id_idx                ON rdsconfig (organization_id);

CREATE UNIQUE INDEX IF NOT EXISTS       imagecatalog_id_idx                 ON imagecatalog (id);
CREATE INDEX IF NOT EXISTS              imagecatalog_name_idx               ON imagecatalog (name);
CREATE INDEX IF NOT EXISTS              imagecatalog_org_id_idx             ON imagecatalog (organization_id);

CREATE UNIQUE INDEX IF NOT EXISTS       proxyconfig_id_idx                  ON proxyconfig (id);
CREATE INDEX IF NOT EXISTS              proxyconfig_name_idx                ON proxyconfig (name);
CREATE INDEX IF NOT EXISTS              proxyconfig_org_id_idx              ON proxyconfig (organization_id);


-- //@UNDO
-- SQL to undo the change goes here.

-- re-adding NOT NULL constraint is not possible, cannot repopulate NULLs with confidence
