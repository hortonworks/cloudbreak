-- // RMP-12890
-- Migration SQL that makes the change goes here.

UPDATE kerberosconfig SET type = 'ACTIVE_DIRECTORY' WHERE type LIKE 'EXISTING_AD';
UPDATE kerberosconfig SET type = 'MIT' WHERE type LIKE 'EXISTING_MIT';
UPDATE kerberosconfig SET type = 'FREEIPA' WHERE type LIKE 'EXISTING_FREEIPA';
UPDATE kerberosconfig SET type = 'AMBARI_DESCRIPTOR' WHERE type LIKE 'CUSTOM';
UPDATE cluster SET kerberosconfig_id = null WHERE kerberosconfig_id = (SELECT id FROM kerberosconfig WHERE id = kerberosconfig_id AND type LIKE 'CB_MANAGED');
UPDATE cluster SET secure = false WHERE kerberosconfig_id = (SELECT id FROM kerberosconfig WHERE id = kerberosconfig_id AND type LIKE 'CB_MANAGED');
DELETE FROM kerberosconfig WHERE type LIKE 'CB_MANAGED';

CREATE TABLE IF NOT EXISTS env_kdc (
    envid bigint NOT NULL,
    kdcid bigint NOT NULL
);
ALTER TABLE ONLY env_kdc ADD CONSTRAINT fk_env_kdc_envid FOREIGN KEY (envid) REFERENCES environment(id);
ALTER TABLE ONLY env_kdc ADD CONSTRAINT fk_env_kdc_kdcid FOREIGN KEY (kdcid) REFERENCES kerberosconfig(id);
ALTER TABLE ONLY env_kdc ADD CONSTRAINT uk_env_kdc_envid_kdcid UNIQUE (envid, kdcid);
CREATE INDEX IF NOT EXISTS idx_env_kdc_envid ON env_kdc (envid);
CREATE INDEX IF NOT EXISTS idx_env_kdc_kdcid ON env_kdc (kdcid);

ALTER TABLE kerberosconfig ADD name character varying(255);
UPDATE kerberosconfig SET name = CONCAT('kerberos-', id);
ALTER TABLE kerberosconfig ALTER COLUMN name SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_kerberosconfig_workspace_id_name ON kerberosconfig (workspace_id, name);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_kerberosconfig_workspace_id_name;
ALTER TABLE kerberosconfig DROP COLUMN IF EXISTS name;

DROP INDEX IF EXISTS idx_env_kdc_envid;
DROP INDEX IF EXISTS idx_env_kdc_kdcid;
ALTER TABLE ONLY env_kdc DROP CONSTRAINT IF EXISTS uk_env_kdc_envid_kdcid;
ALTER TABLE ONLY env_kdc DROP CONSTRAINT IF EXISTS fk_env_kdc_kdcid;
ALTER TABLE ONLY env_kdc DROP CONSTRAINT IF EXISTS fk_env_kdc_envid;
DROP TABLE IF EXISTS env_kdc;

UPDATE kerberosconfig SET type = 'CUSTOM' WHERE type LIKE 'AMBARI_DESCRIPTOR';
UPDATE kerberosconfig SET type = 'EXISTING_FREEIPA' WHERE type LIKE 'FREEIPA';
UPDATE kerberosconfig SET type = 'EXISTING_MIT' WHERE type LIKE 'MIT';
UPDATE kerberosconfig SET type = 'EXISTING_AD' WHERE type LIKE 'ACTIVE_DIRECTORY';

