-- // CB-1049 Environment aware resources to archivable
-- Migration SQL that makes the change goes here.
ALTER TABLE IF EXISTS rdsconfig
    ADD COLUMN IF NOT EXISTS archived boolean DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deletionTimestamp BIGINT DEFAULT -1,
    DROP CONSTRAINT rdsconfigname_in_org_unique,
    ADD CONSTRAINT uk_rdsconfig_deletiondate_workspace UNIQUE (name, deletionTimestamp, workspace_id);

ALTER TABLE IF EXISTS proxyconfig
    ADD COLUMN IF NOT EXISTS archived boolean DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deletionTimestamp BIGINT DEFAULT -1,
    DROP CONSTRAINT proxyconfigname_in_org_unique,
    ADD CONSTRAINT uk_proxyconfig_deletiondate_workspace UNIQUE (name, deletionTimestamp, workspace_id);

ALTER TABLE IF EXISTS ldapconfig
    ADD COLUMN IF NOT EXISTS archived boolean DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deletionTimestamp BIGINT DEFAULT -1,
    DROP CONSTRAINT ldapconfigname_in_org_unique,
    ADD CONSTRAINT uk_ldapconfig_deletiondate_workspace UNIQUE (name, deletionTimestamp, workspace_id);

DROP INDEX IF EXISTS idx_kerberosconfig_workspace_id_name;
ALTER TABLE IF EXISTS kerberosconfig
    ADD COLUMN IF NOT EXISTS archived boolean DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deletionTimestamp BIGINT DEFAULT -1,
    ADD CONSTRAINT uk_kerberosconfig_deletiondate_workspace UNIQUE (name, deletionTimestamp, workspace_id);

ALTER TABLE IF EXISTS kubernetesconfig
    ADD COLUMN IF NOT EXISTS archived boolean DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deletionTimestamp BIGINT DEFAULT -1,
    DROP CONSTRAINT IF EXISTS kubernetesconfigname_in_org_unique,
    ADD CONSTRAINT uk_kubernetesconfig_deletiondate_workspace UNIQUE (name, deletionTimestamp, workspace_id);

-- //@UNDO
-- SQL to undo the change goes here.

DELETE FROM cluster_rdsconfig WHERE rdsconfigs_id IN (SELECT id FROM rdsconfig WHERE archived=true);
DELETE FROM datalakeresources_rdsconfig WHERE rdsconfigs_id IN (SELECT id FROM rdsconfig WHERE archived=true);
DELETE FROM env_rds WHERE rdsid IN (SELECT id FROM rdsconfig WHERE archived=true);
DELETE FROM rdsconfig WHERE archived=true;
ALTER TABLE rdsconfig
    DROP CONSTRAINT IF EXISTS uk_rdsconfig_deletiondate_workspace,
    ADD CONSTRAINT rdsconfigname_in_org_unique UNIQUE (name, workspace_id),
    DROP COLUMN IF EXISTS deletionTimestamp,
    DROP COLUMN IF EXISTS archived;


UPDATE cluster SET proxyconfig_id=null WHERE proxyconfig_id IN (SELECT id FROM proxyconfig WHERE archived=true);
DELETE FROM env_proxy WHERE proxyid IN (SELECT id FROM proxyconfig WHERE archived=true);
DELETE FROM proxyconfig WHERE archived=true;
ALTER TABLE proxyconfig
    DROP CONSTRAINT IF EXISTS uk_proxyconfig_deletiondate_workspace,
    ADD CONSTRAINT proxyconfigname_in_org_unique UNIQUE (name, workspace_id),
    DROP COLUMN IF EXISTS deletionTimestamp,
    DROP COLUMN IF EXISTS archived;


UPDATE cluster SET ldapconfig_id=null WHERE ldapconfig_id IN (SELECT id FROM ldapconfig WHERE archived=true);
UPDATE datalakeresources SET ldapconfig_id=null WHERE ldapconfig_id IN (SELECT id FROM ldapconfig WHERE archived=true);
DELETE FROM env_ldap WHERE ldapid IN (SELECT id FROM ldapconfig WHERE archived=true);
DELETE FROM ldapconfig WHERE archived=true;
ALTER TABLE ldapconfig
    DROP CONSTRAINT IF EXISTS uk_ldapconfig_deletiondate_workspace,
    ADD CONSTRAINT ldapconfigname_in_org_unique UNIQUE (name, workspace_id),
    DROP COLUMN IF EXISTS deletionTimestamp,
    DROP COLUMN IF EXISTS archived;

UPDATE cluster SET kerberosconfig_id=null WHERE kerberosconfig_id IN (SELECT id FROM kerberosconfig WHERE archived=true);
UPDATE datalakeresources SET kerberosconfig_id=null WHERE kerberosconfig_id IN (SELECT id FROM kerberosconfig WHERE archived=true);
DELETE FROM env_kdc WHERE kdcid IN (SELECT id FROM kerberosconfig WHERE archived=true);
DELETE FROM kerberosconfig WHERE archived=true;
CREATE UNIQUE INDEX idx_kerberosconfig_workspace_id_name ON kerberosconfig(name text_ops,workspace_id int8_ops);
ALTER TABLE kerberosconfig
    DROP CONSTRAINT IF EXISTS uk_kerberosconfig_deletiondate_workspace,
    DROP COLUMN IF EXISTS deletionTimestamp,
    DROP COLUMN IF EXISTS archived;

DELETE FROM env_kubernetes WHERE kubernetesid IN (SELECT id FROM kubernetesconfig WHERE archived=true);
DELETE FROM kubernetesconfig WHERE archived=true;
ALTER TABLE kubernetesconfig
    DROP CONSTRAINT IF EXISTS uk_kubernetesconfig_deletiondate_workspace,
    ADD CONSTRAINT kubernetesconfigname_in_org_unique UNIQUE (name, workspace_id),
    DROP COLUMN IF EXISTS deletionTimestamp,
    DROP COLUMN IF EXISTS archived;
