-- // RMP-12890
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS env_kdc (
    envid bigint NOT NULL,
    kdcid bigint NOT NULL
);
ALTER TABLE ONLY env_kdc ADD CONSTRAINT fk_env_kdc_envid FOREIGN KEY (envid) REFERENCES environment(id);
ALTER TABLE ONLY env_kdc ADD CONSTRAINT fk_env_kdc_kdcid FOREIGN KEY (kdcid) REFERENCES kerberosconfig(id);
ALTER TABLE ONLY env_kdc ADD CONSTRAINT uk_env_kdc_envid_kdcid UNIQUE (envid, kdcid);
CREATE INDEX IF NOT EXISTS idx_env_kdc_envid ON env_kdc (envid);
CREATE INDEX IF NOT EXISTS idx_env_kdc_kdcid ON env_kdc (kdcid);

ALTER TABLE kerberosconfig ADD name character varying(255) NOT NULL;
CREATE INDEX IF NOT EXISTS idx_kerberosconfig_workspace_id_name ON kerberosconfig (workspace_id, name);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_kerberosconfig_workspace_id_name;
ALTER TABLE kerberosconfig DROP COLUMN name;

DROP INDEX IF EXISTS idx_env_kdc_envid;
DROP INDEX IF EXISTS idx_env_kdc_kdcid;
ALTER TABLE ONLY env_kdc DROP CONSTRAINT IF EXISTS uk_env_kdc_envid_kdcid;
ALTER TABLE ONLY env_kdc DROP CONSTRAINT IF EXISTS fk_env_kdc_kdcid;
ALTER TABLE ONLY env_kdc DROP CONSTRAINT IF EXISTS fk_env_kdc_envid;
DROP TABLE IF EXISTS env_kdc;

