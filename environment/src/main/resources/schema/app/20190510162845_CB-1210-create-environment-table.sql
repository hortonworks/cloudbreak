-- // CB-1210 create environment table
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS environment (
    id                      bigserial NOT NULL,
    name                    character varying(255) NOT NULL,
    description             text,
    cloudplatform           character varying(255) NOT NULL,
    credential_id           bigint NOT NULL,
    accountid               varchar(255) NOT NULL,
    regions                 text NOT NULL,
    location                character varying(255),
    longitude               double precision,
    latitude                double precision,
    datalakeresources_id    bigint,
    locationdisplayname     character varying(255),
    archived                boolean DEFAULT false,
    deletiontimestamp       bigint DEFAULT '-1'::integer,
    CONSTRAINT uk_environment_deletiondate_accountid UNIQUE (name, deletiontimestamp, accountid),
    CONSTRAINT fk_environment_credential_id FOREIGN KEY (credential_id) REFERENCES credential(id),
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS environment_credential_id_idx ON environment USING btree (credential_id);
CREATE INDEX IF NOT EXISTS environment_accountid_name_idx ON environment USING btree (accountid, name);
CREATE INDEX IF NOT EXISTS environment_id_accountid_idx ON credential USING btree (id, accountid);


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

DROP INDEX IF EXISTS environment_id_accountid_idx;
DROP INDEX IF EXISTS env_proxy_envid_idx;
DROP INDEX IF EXISTS env_proxy_proxyid_idx;
DROP TABLE IF EXISTS env_proxy;

DROP INDEX IF EXISTS idx_environment_credential_id;
DROP INDEX IF EXISTS idx_environment_accountid_name;
DROP TABLE IF EXISTS environment;