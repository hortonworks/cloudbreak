-- // CB-1225 environment specific network entity
-- Migration SQL that makes the change goes here.

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
    accountid           varchar(255) NOT NULL,
    network_platform    character varying(255),
    CONSTRAINT environment_network_pkey PRIMARY KEY (id),
    CONSTRAINT fk_environment_network_environment FOREIGN KEY (environment_id) REFERENCES environment(id)
);

CREATE INDEX IF NOT EXISTS environment_network_accountid_name_idx ON environment_network (accountid, name);
CREATE INDEX IF NOT EXISTS environment_network_environment_id_idx ON environment_network (environment_id);


-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS environment_network_environment_id_idx;
DROP INDEX IF EXISTS environment_network_accountid_name_idx;

ALTER TABLE ONLY environment_network DROP CONSTRAINT IF EXISTS environment_network_pkey;

DROP TABLE IF EXISTS environment_network;
