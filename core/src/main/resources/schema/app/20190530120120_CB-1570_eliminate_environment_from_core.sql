-- // CB-1570 eliminating environment from core
-- Migration SQL that makes the change goes here.

ALTER TABLE stack DROP COLUMN IF EXISTS environment_id;
ALTER TABLE cluster DROP COLUMN IF EXISTS environment_id;
ALTER TABLE datalakeresources DROP COLUMN IF EXISTS environment_id;

ALTER TABLE stack ADD COLUMN IF NOT EXISTS environmentcrn varchar(255);
ALTER TABLE cluster ADD COLUMN IF NOT EXISTS environmentcrn varchar(255);
ALTER TABLE datalakeresources ADD COLUMN IF NOT EXISTS environmentcrn varchar(255);

CREATE INDEX idx_stack_envcrn_name ON stack USING btree (environmentcrn, name);
CREATE INDEX idx_cluster_envcrn_name ON cluster USING btree (environmentcrn, name);
CREATE INDEX idx_datalakeresources_envcrn_name ON datalakeresources USING btree (environmentcrn, name);

DROP TABLE IF EXISTS environment_network;
DROP TABLE IF EXISTS env_ldap;
DROP TABLE IF EXISTS env_proxy;
DROP TABLE IF EXISTS environment;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS environmentcrn;
ALTER TABLE stack DROP COLUMN IF EXISTS environmentcrn;
ALTER TABLE datalakeresources DROP COLUMN IF EXISTS environmentcrn;

