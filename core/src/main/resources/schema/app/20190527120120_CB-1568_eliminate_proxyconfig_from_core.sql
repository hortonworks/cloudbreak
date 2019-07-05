-- // CB-1568 modify cluster to refer to a proxy crn instead of local entity
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS proxyconfigcrn varchar(255);
ALTER TABLE cluster DROP COLUMN IF EXISTS proxyconfig_id;

DROP TABLE IF EXISTS proxyconfig;

-- //@UNDO
-- SQL to undo the change goes here.

CREATE TABLE IF NOT EXISTS proxyconfig (
    id bigserial NOT NULL,
    account character varying(255),
    owner character varying(255),
    name character varying(255) NOT NULL,
    serverhost character varying(255) NOT NULL,
    serverport integer NOT NULL,
    protocol character varying(255) NOT NULL,
    username character varying(255),
    password character varying(255),
    publicinaccount boolean DEFAULT false NOT NULL,
    description text,
    workspace_id bigint,
    archived boolean DEFAULT false,
    deletiontimestamp bigint DEFAULT '-1'::integer,
    PRIMARY KEY (id),
    CONSTRAINT fk_proxyconfig_organization FOREIGN KEY (workspace_id) REFERENCES workspace(id),
    CONSTRAINT uk_proxyconfig_deletiondate_workspace UNIQUE (name, deletiontimestamp, workspace_id)
);

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS proxyconfig_id BIGINT REFERENCES proxyconfig(id);
ALTER TABLE cluster DROP COLUMN IF EXISTS proxyconfigcrn;

CREATE INDEX IF NOT EXISTS idx_proxyconfig_workspace_id_name ON proxyconfig USING btree (workspace_id, name);
CREATE INDEX IF NOT EXISTS proxyconfig_name_idx ON proxyconfig USING btree (name);
CREATE INDEX IF NOT EXISTS proxyconfig_org_id_idx ON proxyconfig USING btree (workspace_id);
CREATE UNIQUE INDEX IF NOT EXISTS proxyconfig_id_idx ON proxyconfig USING btree (id);
