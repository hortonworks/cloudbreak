-- // CB-1210 create environment table
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS environment (
    id                      bigserial NOT NULL,
    name                    character varying(255) NOT NULL,
    description             text,
    cloudplatform           character varying(255) NOT NULL,
    credential_id           bigint NOT NULL,
    workspace_id            bigint NOT NULL,
    regions                 text NOT NULL,
    location                character varying(255),
    longitude               double precision,
    latitude                double precision,
    datalakeresources_id    bigint,
    locationdisplayname     character varying(255),
    archived                boolean DEFAULT false,
    deletiontimestamp       bigint DEFAULT '-1'::integer,
    CONSTRAINT uk_environment_deletiondate_workspace UNIQUE (name, deletiontimestamp, workspace_id),
    CONSTRAINT fk_environment_credential_id FOREIGN KEY (credential_id) REFERENCES credential(id),
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_environment_credential_id ON environment USING btree (credential_id);
CREATE INDEX IF NOT EXISTS idx_environment_workspace_id_name ON environment USING btree (workspace_id, name);


CREATE TABLE IF NOT EXISTS env_proxy (
    envid bigint NOT NULL,
    proxyid bigint NOT NULL,
    CONSTRAINT fk_env_proxy_envid FOREIGN KEY (envid) REFERENCES environment(id),
    CONSTRAINT fk_env_proxy_proxyid FOREIGN KEY (proxyid) REFERENCES proxyconfig(id),
    CONSTRAINT uk_env_proxy_envid_proxyid UNIQUE (envid, proxyid)
);
CREATE INDEX IF NOT EXISTS idx_env_proxy_envid ON env_proxy (envid);
CREATE INDEX IF NOT EXISTS idx_env_proxy_proxyid ON env_proxy (proxyid);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_env_proxy_envid;
DROP INDEX IF EXISTS idx_env_proxy_proxyid;
DROP TABLE IF EXISTS env_proxy;

DROP INDEX IF EXISTS idx_environment_credential_id;
DROP INDEX IF EXISTS idx_environment_workspace_id_name;
DROP TABLE IF EXISTS environment;