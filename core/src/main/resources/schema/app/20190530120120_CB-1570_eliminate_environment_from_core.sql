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

ALTER TABLE environment ADD COLUMN IF NOT EXISTS locationdisplayname varchar(255);
ALTER TABLE environment ADD COLUMN IF NOT EXISTS location varchar(255);
ALTER TABLE environment ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
ALTER TABLE environment ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE environment ADD COLUMN IF NOT EXISTS datalakeresources_id BIGINT;



ALTER TABLE stack ADD COLUMN IF NOT EXISTS environment_id bigint;
ALTER TABLE ONLY stack ADD CONSTRAINT fk_stack_environment FOREIGN KEY (environment_id) REFERENCES environment(id);
CREATE INDEX IF NOT EXISTS idx_stack_environment_id ON stack(environment_id);

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS environment_id bigint;
ALTER TABLE ONLY cluster ADD CONSTRAINT fk_cluster_environment FOREIGN KEY (environment_id) REFERENCES environment(id);
CREATE INDEX IF NOT EXISTS idx_cluster_environment_id ON cluster(environment_id);

ALTER TABLE datalakeresources ADD COLUMN IF NOT EXISTS environment_id bigint;
ALTER TABLE ONLY datalakeresources ADD CONSTRAINT fk_datalakeresources_environment FOREIGN KEY (environment_id) REFERENCES environment(id);
CREATE INDEX IF NOT EXISTS idx_datalakeresources_environment_id ON datalakeresources(environment_id);






CREATE TABLE IF NOT EXISTS environment_network
(
    id                  bigserial NOT NULL,
    name                character varying(255) NOT NULL,
    archived            boolean NOT NULL DEFAULT false,
    deletiontimestamp   bigint NOT NULL,
    subnetids           TEXT NOT NULL,
    vpcid               character varying(255),
    networkid           character varying(255),
    resourcegroupname   character varying(255),
    nopublicip          boolean DEFAULT false,
    nofirewallrules     boolean DEFAULT false,
    environment_id      int8 NOT NULL,
    workspace_id        int8 NOT NULL,
    network_platform    character varying(255),
    CONSTRAINT environment_network_pkey PRIMARY KEY (id),
    CONSTRAINT fk_environment_network_environment FOREIGN KEY (environment_id) REFERENCES environment(id),
    CONSTRAINT fk_environment_network_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id)
);

CREATE INDEX IF NOT EXISTS idx_environment_network_workspace_id_name ON environment_network (workspace_id, name);
CREATE INDEX IF NOT EXISTS idx_environment_network_environment_id ON environment_network (environment_id);
