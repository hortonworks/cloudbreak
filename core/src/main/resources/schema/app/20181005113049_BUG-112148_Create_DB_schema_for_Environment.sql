-- // BUG-112148 Create DB schema for Environment
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS environment (
    id bigserial NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    cloudplatform character varying(255) NOT NULL,
    credential_id bigint NOT NULL,
    workspace_id bigint NOT NULL,
    regions TEXT NOT NULL
);
ALTER TABLE ONLY environment ADD CONSTRAINT environment_pkey PRIMARY KEY (id);
ALTER TABLE ONLY environment ADD CONSTRAINT uk_environment_workspace_name UNIQUE (workspace_id, name);
CREATE INDEX IF NOT EXISTS idx_environment_workspace_id_name ON environment (workspace_id, name);

CREATE TABLE IF NOT EXISTS env_ldap (
    envid bigint NOT NULL,
    ldapid bigint NOT NULL
);
ALTER TABLE ONLY env_ldap ADD CONSTRAINT fk_env_ldap_envid FOREIGN KEY (envid) REFERENCES environment(id);
ALTER TABLE ONLY env_ldap ADD CONSTRAINT fk_env_ldap_ldapid FOREIGN KEY (ldapid) REFERENCES ldapconfig(id);
ALTER TABLE ONLY env_ldap ADD CONSTRAINT uk_env_ldap_envid_ldapid UNIQUE (envid, ldapid);
CREATE INDEX IF NOT EXISTS idx_env_ldap_envid ON env_ldap (envid);
CREATE INDEX IF NOT EXISTS idx_env_ldap_ldapid ON env_ldap (ldapid);

CREATE TABLE IF NOT EXISTS env_proxy (
    envid bigint NOT NULL,
    proxyid bigint NOT NULL
);
ALTER TABLE ONLY env_proxy ADD CONSTRAINT fk_env_proxy_envid FOREIGN KEY (envid) REFERENCES environment(id);
ALTER TABLE ONLY env_proxy ADD CONSTRAINT fk_env_proxy_proxyid FOREIGN KEY (proxyid) REFERENCES proxyconfig(id);
ALTER TABLE ONLY env_proxy ADD CONSTRAINT uk_env_proxy_envid_proxyid UNIQUE (envid, proxyid);
CREATE INDEX IF NOT EXISTS idx_env_proxy_envid ON env_proxy (envid);
CREATE INDEX IF NOT EXISTS idx_env_proxy_proxyid ON env_proxy (proxyid);

CREATE TABLE IF NOT EXISTS env_rds (
    envid bigint NOT NULL,
    rdsid bigint NOT NULL
);
ALTER TABLE ONLY env_rds ADD CONSTRAINT fk_env_rds_envid FOREIGN KEY (envid) REFERENCES environment(id);
ALTER TABLE ONLY env_rds ADD CONSTRAINT fk_env_rds_rdsid FOREIGN KEY (rdsid) REFERENCES rdsconfig(id);
ALTER TABLE ONLY env_rds ADD CONSTRAINT uk_env_rds_envid_rdsid UNIQUE (envid, rdsid);
CREATE INDEX IF NOT EXISTS idx_env_rds_envid ON env_rds (envid);
CREATE INDEX IF NOT EXISTS idx_env_rds_rdsid ON env_rds (rdsid);

CREATE INDEX IF NOT EXISTS idx_ldapconfig_workspace_id_name ON ldapconfig (workspace_id, name);
CREATE INDEX IF NOT EXISTS idx_proxyconfig_workspace_id_name ON proxyconfig (workspace_id, name);
CREATE INDEX IF NOT EXISTS idx_rdsconfig_workspace_id_name ON rdsconfig (workspace_id, name);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_rdsconfig_workspace_id_name;
DROP INDEX IF EXISTS idx_proxyconfig_workspace_id_name;
DROP INDEX IF EXISTS idx_ldapconfig_workspace_id_name;

DROP INDEX IF EXISTS idx_env_rds_rdsid;
DROP INDEX IF EXISTS idx_env_rds_envid;
ALTER TABLE ONLY env_rds DROP CONSTRAINT IF EXISTS uk_env_rds_envid_rdsid;
ALTER TABLE ONLY env_rds DROP CONSTRAINT IF EXISTS fk_env_rds_rdsid;
ALTER TABLE ONLY env_rds DROP CONSTRAINT IF EXISTS fk_env_rds_envid;
DROP TABLE IF EXISTS env_rds;

DROP INDEX IF EXISTS idx_env_proxy_proxyid;
DROP INDEX IF EXISTS idx_env_proxy_envid;
ALTER TABLE ONLY env_proxy DROP CONSTRAINT IF EXISTS uk_env_proxy_envid_proxyid;
ALTER TABLE ONLY env_proxy DROP CONSTRAINT IF EXISTS fk_env_proxy_proxyid;
ALTER TABLE ONLY env_proxy DROP CONSTRAINT IF EXISTS fk_env_proxy_envid;
DROP TABLE IF EXISTS env_proxy;

DROP INDEX IF EXISTS idx_env_ldap_ldapid;
DROP INDEX IF EXISTS idx_env_ldap_envid;
ALTER TABLE ONLY env_ldap DROP CONSTRAINT IF EXISTS uk_env_ldap_envid_ldapid;
ALTER TABLE ONLY env_ldap DROP CONSTRAINT IF EXISTS fk_env_ldap_ldapid;
ALTER TABLE ONLY env_ldap DROP CONSTRAINT IF EXISTS fk_env_ldap_envid;
DROP TABLE IF EXISTS env_ldap;

DROP INDEX IF EXISTS idx_environment_workspace_id_name;
ALTER TABLE ONLY environment DROP CONSTRAINT IF EXISTS uk_environment_workspace_name;
ALTER TABLE ONLY environment DROP CONSTRAINT IF EXISTS environment_pkey;
DROP TABLE IF EXISTS environment;
