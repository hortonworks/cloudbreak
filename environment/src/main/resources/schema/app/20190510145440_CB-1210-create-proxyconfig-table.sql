-- // CB-1210 create proxyconfig table
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS proxyconfig (
    id                  bigserial NOT NULL,
    name                character varying(255) NOT NULL,
    serverhost          character varying(255) NOT NULL,
    serverport          integer NOT NULL,
    protocol            character varying(255) NOT NULL,
    username            character varying(255),
    password            character varying(255),
    description         text,
    accountid           varchar(255) NOT NULL,
    archived            boolean DEFAULT false,
    deletiontimestamp   bigint DEFAULT '-1'::integer,
    CONSTRAINT uk_proxyconfig_deletiondate_accountid UNIQUE (name, deletiontimestamp, accountid),
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS proxyconfig_id_idx ON proxyconfig USING btree (id);
CREATE INDEX IF NOT EXISTS proxyconfig_accountid_name_idx ON proxyconfig USING btree (accountid, name);
CREATE INDEX IF NOT EXISTS proxyconfig_name_idx ON proxyconfig USING btree (name);
CREATE INDEX IF NOT EXISTS proxyconfig_accountid_idx ON proxyconfig USING btree (accountid);
CREATE INDEX IF NOT EXISTS proxyconfig_id_accountid_idx ON credential USING btree (id, accountid);


-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS proxyconfig_id_idx;
DROP INDEX IF EXISTS proxyconfig_accountid_name_idx;
DROP INDEX IF EXISTS proxyconfig_name_idx;
DROP INDEX IF EXISTS proxyconfig_accountid_idx;
DROP INDEX IF EXISTS proxyconfig_id_accountid_idx;

DROP TABLE IF EXISTS proxyconfig;