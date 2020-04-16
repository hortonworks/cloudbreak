-- // CB-6512 Delete not used indices
-- Migration SQL that makes the change goes here.

DROP INDEX IF EXISTS blueprint_id_idx;
DROP INDEX IF EXISTS cluster_id_idx;
DROP INDEX IF EXISTS idx_cluster_envcrn_name;
DROP INDEX IF EXISTS filesystem_id_idx;
DROP INDEX IF EXISTS filesystem_name_idx;
DROP INDEX IF EXISTS flexsubscription_id_idx;
DROP INDEX IF EXISTS flexsubscription_name_idx;
DROP INDEX IF EXISTS flexsubscription_org_id_idx;
DROP INDEX IF EXISTS imagecatalog_id_idx;
DROP INDEX IF EXISTS imagecatalog_name_idx;
DROP INDEX IF EXISTS managementpack_id_idx;
DROP INDEX IF EXISTS managementpack_name_idx;
DROP INDEX IF EXISTS network_id_idx;
DROP INDEX IF EXISTS network_name_idx;
DROP INDEX IF EXISTS rdsconfig_id_idx;
DROP INDEX IF EXISTS rdsconfig_name_idx;
DROP INDEX IF EXISTS recipe_id_idx;
DROP INDEX IF EXISTS securitygroup_id_idx;
DROP INDEX IF EXISTS securitygroup_name_idx;
DROP INDEX IF EXISTS smartsensesubscription_subscriptionid_idx;
DROP INDEX IF EXISTS smartsensesubscription_org_id_idx;
DROP INDEX IF EXISTS smartsensesubscription_id_idx;
DROP INDEX IF EXISTS stack_id_idx;
DROP INDEX IF EXISTS stack_name_idx;
DROP INDEX IF EXISTS template_id_idx;
DROP INDEX IF EXISTS template_name_idx;
DROP INDEX IF EXISTS tenant_id_idx;
DROP INDEX IF EXISTS tenant_name_idx;
DROP INDEX IF EXISTS topology_id_idx;
DROP INDEX IF EXISTS topology_name_idx;
DROP INDEX IF EXISTS userprofile_user_id_idx;
DROP INDEX IF EXISTS users_id_idx;
DROP INDEX IF EXISTS organization_id_idx;
DROP INDEX IF EXISTS organization_name_idx;
DROP INDEX IF EXISTS organization_deletiontimestamp_idx;

-- //@UNDO
-- SQL to undo the change goes here.

CREATE UNIQUE INDEX IF NOT EXISTS       blueprint_id_idx                    ON blueprint (id);
CREATE UNIQUE INDEX IF NOT EXISTS       cluster_id_idx                      ON cluster (id);
CREATE INDEX IF NOT EXISTS              idx_cluster_envcrn_name ON cluster USING btree (environmentcrn, name);
CREATE UNIQUE INDEX IF NOT EXISTS       filesystem_id_idx                   ON filesystem (id);
CREATE INDEX IF NOT EXISTS              filesystem_name_idx                 ON filesystem (name);
CREATE UNIQUE INDEX IF NOT EXISTS       flexsubscription_id_idx             ON flexsubscription (id);
CREATE INDEX IF NOT EXISTS              flexsubscription_name_idx           ON flexsubscription (name);
CREATE INDEX IF NOT EXISTS              flexsubscription_org_id_idx         ON flexsubscription (workspace_id);
CREATE UNIQUE INDEX IF NOT EXISTS       imagecatalog_id_idx                 ON imagecatalog (id);
CREATE INDEX IF NOT EXISTS              imagecatalog_name_idx               ON imagecatalog (name);
CREATE UNIQUE INDEX IF NOT EXISTS       managementpack_id_idx               ON managementpack (id);
CREATE INDEX IF NOT EXISTS              managementpack_name_idx             ON managementpack (name);
CREATE UNIQUE INDEX IF NOT EXISTS       network_id_idx                      ON network (id);
CREATE INDEX IF NOT EXISTS              network_name_idx                    ON network (name);
CREATE UNIQUE INDEX IF NOT EXISTS       rdsconfig_id_idx                    ON rdsconfig (id);
CREATE INDEX IF NOT EXISTS              rdsconfig_name_idx                  ON rdsconfig (name);
CREATE UNIQUE INDEX IF NOT EXISTS       recipe_id_idx                       ON recipe (id);
CREATE UNIQUE INDEX IF NOT EXISTS       securitygroup_id_idx                ON securitygroup (id);
CREATE INDEX IF NOT EXISTS              securitygroup_name_idx              ON securitygroup (name);
CREATE UNIQUE INDEX IF NOT EXISTS       smartsensesubscription_id_idx       ON smartsensesubscription (id);
CREATE INDEX IF NOT EXISTS              smartsensesubscription_subscriptionid_idx ON smartsensesubscription (subscriptionid);
CREATE INDEX IF NOT EXISTS              smartsensesubscription_org_id_idx   ON smartsensesubscription (workspace_id);
CREATE UNIQUE INDEX IF NOT EXISTS       stack_id_idx                        ON stack (id);
CREATE INDEX IF NOT EXISTS              stack_name_idx                      ON stack (name);
CREATE UNIQUE INDEX IF NOT EXISTS       template_id_idx                     ON template (id);
CREATE INDEX IF NOT EXISTS              template_name_idx                   ON template (name);
CREATE UNIQUE INDEX IF NOT EXISTS       tenant_id_idx                       ON tenant (id);
CREATE UNIQUE INDEX IF NOT EXISTS       tenant_name_idx                     ON tenant (name);
CREATE UNIQUE INDEX IF NOT EXISTS       topology_id_idx                     ON topology (id);
CREATE INDEX IF NOT EXISTS              topology_name_idx                   ON topology (name);
CREATE INDEX IF NOT EXISTS              userprofile_user_id_idx             ON userprofile (user_id);
CREATE UNIQUE INDEX IF NOT EXISTS       users_id_idx                        ON users (id);
CREATE UNIQUE INDEX IF NOT EXISTS       organization_id_idx                 ON workspace (id);
CREATE INDEX IF NOT EXISTS              organization_name_idx               ON workspace (name);
CREATE INDEX IF NOT EXISTS              organization_deletiontimestamp_idx  ON workspace (deletiontimestamp);
