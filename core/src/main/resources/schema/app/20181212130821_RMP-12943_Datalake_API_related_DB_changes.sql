-- // RMP-12943 Datalake API related DB changes
-- Migration SQL that makes the change goes here.

DROP SEQUENCE IF EXISTS datalakeresources_datalakestack_id_seq CASCADE;
ALTER TABLE datalakeresources ALTER COLUMN datalakestack_id DROP NOT NULL;

ALTER TABLE datalakeresources ADD COLUMN IF NOT EXISTS ldapconfig_id BIGINT;
ALTER TABLE datalakeresources ADD COLUMN IF NOT EXISTS kerberosconfig_id BIGINT;

ALTER TABLE ONLY datalakeresources ADD CONSTRAINT fk_datalakeresources_ldapconfig_id FOREIGN KEY (ldapconfig_id) REFERENCES ldapconfig(id);
ALTER TABLE ONLY datalakeresources ADD CONSTRAINT fk_datalakeresources_kerberosconfig_id FOREIGN KEY (kerberosconfig_id) REFERENCES kerberosconfig(id);

ALTER TABLE environment ADD COLUMN IF NOT EXISTS datalakeresources_id BIGINT;

CREATE TABLE datalakeresources_rdsconfig (
    datalakeresources_id bigint NOT NULL,
    rdsconfigs_id bigint NOT NULL
);

ALTER TABLE ONLY datalakeresources_rdsconfig ADD CONSTRAINT datalakeresources_rdsconfig_pkey PRIMARY KEY (datalakeresources_id, rdsconfigs_id);
ALTER TABLE ONLY datalakeresources_rdsconfig ADD CONSTRAINT fk_datalakeresources_rdsconfig_datalakeresources_id FOREIGN KEY (datalakeresources_id) REFERENCES datalakeresources(id);
ALTER TABLE ONLY datalakeresources_rdsconfig ADD CONSTRAINT fk_datalakeresources_rdsconfig_rdsconfig_id FOREIGN KEY (rdsconfigs_id) REFERENCES rdsconfig(id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE IF EXISTS datalakeresources_rdsconfig;

ALTER TABLE environment DROP COLUMN IF EXISTS datalakeresources_id;

ALTER TABLE datalakeresources DROP COLUMN IF EXISTS ldapconfig_id;

ALTER TABLE ONLY datalakeresources DROP CONSTRAINT IF EXISTS fk_datalakeresources_kerberosconfig_id;
ALTER TABLE ONLY datalakeresources DROP CONSTRAINT IF EXISTS fk_datalakeresources_ldapconfig_id;

ALTER TABLE datalakeresources DROP COLUMN IF EXISTS ldapconfig_id;
ALTER TABLE datalakeresources DROP COLUMN IF EXISTS kerberosconfig_id;
