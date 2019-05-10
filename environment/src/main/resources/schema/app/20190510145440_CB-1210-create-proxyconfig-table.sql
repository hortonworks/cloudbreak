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
    workspace_id        bigint,
    archived            boolean DEFAULT false,
    deletiontimestamp   bigint DEFAULT '-1'::integer,
    CONSTRAINT uk_proxyconfig_deletiondate_workspace UNIQUE (name, deletiontimestamp, workspace_id),
    CONSTRAINT fk_proxyconfig_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id),
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS proxyconfig_id_idx ON proxyconfig USING btree (id);
CREATE INDEX IF NOT EXISTS idx_proxyconfig_workspace_id_name ON proxyconfig USING btree (workspace_id, name);
CREATE INDEX IF NOT EXISTS proxyconfig_name_idx ON proxyconfig USING btree (name);
CREATE INDEX IF NOT EXISTS proxyconfig_workspace_id_idx ON proxyconfig USING btree (workspace_id);


-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS proxyconfig_id_idx;
DROP INDEX IF EXISTS idx_proxyconfig_workspace_id_name;
DROP INDEX IF EXISTS proxyconfig_name_idx;
DROP INDEX IF EXISTS proxyconfig_workspace_id_idx;

DROP TABLE IF EXISTS proxyconfig;