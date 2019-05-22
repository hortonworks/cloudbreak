-- // CB-1516 remove relation between Environment and LDAP
-- Migration SQL that makes the change goes here.
DROP TABLE IF EXISTS env_ldap;


-- //@UNDO
-- SQL to undo the change goes here.

CREATE TABLE IF NOT EXISTS env_ldap (
    envid bigint NOT NULL,
    ldapid bigint NOT NULL
);
ALTER TABLE ONLY env_ldap ADD CONSTRAINT fk_env_ldap_envid FOREIGN KEY (envid) REFERENCES environment(id);
ALTER TABLE ONLY env_ldap ADD CONSTRAINT fk_env_ldap_ldapid FOREIGN KEY (ldapid) REFERENCES ldapconfig(id);
ALTER TABLE ONLY env_ldap ADD CONSTRAINT uk_env_ldap_envid_ldapid UNIQUE (envid, ldapid);
CREATE INDEX IF NOT EXISTS idx_env_ldap_envid ON env_ldap (envid);
CREATE INDEX IF NOT EXISTS idx_env_ldap_ldapid ON env_ldap (ldapid);


