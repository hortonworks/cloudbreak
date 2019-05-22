-- // CB-1513 remove kdc env relation
-- Migration SQL that makes the change goes here.
DROP TABLE IF EXISTS env_kdc;

 -- //@UNDO
-- SQL to undo the change goes here.

CREATE TABLE IF NOT EXISTS env_kdc (
    envid int8 NOT NULL,
    kdcid int8 NOT NULL
);
ALTER TABLE ONLY env_kdc ADD CONSTRAINT fk_env_kdc_envid FOREIGN KEY (envid) REFERENCES environment(id);
ALTER TABLE ONLY env_kdc ADD CONSTRAINT fk_env_kdc_kdcid FOREIGN KEY (kdcid) REFERENCES kerberosconfig(id);
ALTER TABLE ONLY env_kdc ADD CONSTRAINT uk_env_kdc_envid_kdcid UNIQUE (envid, kdcid);
CREATE INDEX IF NOT EXISTS idx_env_kdc_envid ON env_kdc (envid);
CREATE INDEX IF NOT EXISTS idx_env_kdc_kdcid ON env_kdc (kdcid);