-- // CB-13065 removing datalakeresources and servicedescriptor
-- Migration SQL that makes the change goes here.

ALTER TABLE stack DROP COLUMN datalakeresourceid;

DROP TABLE datalakeresources_rdsconfig;

DROP TABLE servicedescriptor;

DROP TABLE datalakeresources;

-- //@UNDO
-- SQL to undo the change goes here.

CREATE TABLE IF NOT EXISTS datalakeresources (
    id bigserial NOT NULL,
    datalakestack_id bigserial,
    name character varying(255) NOT NULL,
    datalakeambariip character varying(255) NOT NULL,
    datalakeambarifqdn character varying(255) NOT NULL,
    workspace_id bigint NOT NULL,
    datalakecomponents TEXT NOT NULL,
    datalakeclustermanagerip varchar(255),
    datalakeclustermanagerfqdn varchar(255),
    datalakeclustermanagerurl varchar(255),
    environmentcrn varchar(255),
    datalakeambariurl VARCHAR(255),
    kerberosconfig_id BIGINT
);
ALTER TABLE ONLY datalakeresources ADD CONSTRAINT datalakeresources_pkey PRIMARY KEY (id);
ALTER TABLE ONLY datalakeresources ADD CONSTRAINT fk_stack_datalakestack_id FOREIGN KEY (datalakestack_id) REFERENCES stack(id);
ALTER TABLE ONLY datalakeresources ADD CONSTRAINT uk_datalakeresources_workspace_name UNIQUE (workspace_id, name);
CREATE INDEX IF NOT EXISTS idx_datalakeresources_workspace_id_name ON datalakeresources (workspace_id, name);
CREATE INDEX IF NOT EXISTS idx_datalakeresources_datalakestack_id ON datalakeresources (datalakestack_id);

CREATE TABLE IF NOT EXISTS servicedescriptor (
    id bigserial NOT NULL,
    datalakeresources_id bigint NOT NULL,
    workspace_id bigint NOT NULL,
    servicename character varying(255) NOT NULL,
    blueprintparams TEXT NOT NULL,
    blueprintsecretparams TEXT,
    componentshosts TEXT NOT NULL
);
ALTER TABLE ONLY servicedescriptor ADD CONSTRAINT servicedescriptor_pkey PRIMARY KEY (id);
ALTER TABLE ONLY servicedescriptor ADD CONSTRAINT fk_servicedescriptor_datalakeresources_id FOREIGN KEY (datalakeresources_id) REFERENCES datalakeresources(id);
CREATE INDEX IF NOT EXISTS idx_servicedescriptor_datalakeresources_id ON servicedescriptor (datalakeresources_id);

DROP SEQUENCE IF EXISTS datalakeresources_datalakestack_id_seq CASCADE;
ALTER TABLE datalakeresources ALTER COLUMN datalakestack_id DROP NOT NULL;

ALTER TABLE ONLY datalakeresources ADD CONSTRAINT fk_datalakeresources_kerberosconfig_id FOREIGN KEY (kerberosconfig_id) REFERENCES kerberosconfig(id);

CREATE TABLE datalakeresources_rdsconfig (
    datalakeresources_id bigint NOT NULL,
    rdsconfigs_id bigint NOT NULL
);

ALTER TABLE ONLY datalakeresources_rdsconfig ADD CONSTRAINT datalakeresources_rdsconfig_pkey PRIMARY KEY (datalakeresources_id, rdsconfigs_id);
ALTER TABLE ONLY datalakeresources_rdsconfig ADD CONSTRAINT fk_datalakeresources_rdsconfig_datalakeresources_id FOREIGN KEY (datalakeresources_id) REFERENCES datalakeresources(id);
ALTER TABLE ONLY datalakeresources_rdsconfig ADD CONSTRAINT fk_datalakeresources_rdsconfig_rdsconfig_id FOREIGN KEY (rdsconfigs_id) REFERENCES rdsconfig(id);

ALTER TABLE stack ADD COLUMN IF NOT EXISTS datalakeresourceid BIGINT;

CREATE INDEX idx_datalakeresources_envcrn_name ON datalakeresources USING btree (environmentcrn, name);




