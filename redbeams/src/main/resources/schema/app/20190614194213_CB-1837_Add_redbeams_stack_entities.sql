-- // CB-1837 Add redbeams stack entities
-- Migration SQL that makes the change goes here.

CREATE TABLE network
(
    id BIGINT NOT NULL,
    name VARCHAR(255),
    description TEXT,
    attributes TEXT,
    PRIMARY KEY (id)
);

CREATE SEQUENCE network_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE UNIQUE INDEX IF NOT EXISTS network_id_idx ON network(id);
CREATE INDEX IF NOT EXISTS network_name_idx ON network(name);

CREATE TABLE securitygroup
(
    id BIGINT NOT NULL,
    name VARCHAR(255),
    description TEXT,
    PRIMARY KEY (id)
);

CREATE SEQUENCE securitygroup_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE UNIQUE INDEX IF NOT EXISTS securitygroup_id_idx ON securitygroup(id);
CREATE INDEX IF NOT EXISTS securitygroup_name_idx ON securitygroup(name);

CREATE TABLE securitygroup_securitygroupids (
    securitygroup_id bigint NOT NULL REFERENCES securitygroup (id),
    securitygroupid_value text
);

CREATE TABLE databaseserver
(
    id BIGINT NOT NULL,
    name VARCHAR(255),
    description TEXT,
    instancetype VARCHAR(255),
    databasevendor VARCHAR(255),
    storagesize BIGINT,
    rootusername VARCHAR(255) NOT NULL,
    rootpassword VARCHAR(255) NOT NULL,
    securitygroup_id BIGINT REFERENCES securitygroup (id),
    attributes TEXT,
    PRIMARY KEY (id)
);

CREATE SEQUENCE databaseserver_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE UNIQUE INDEX IF NOT EXISTS databaseserver_id_idx ON databaseserver(id);
CREATE INDEX IF NOT EXISTS databaseserver_name_idx ON databaseserver(name);

CREATE TABLE dbstack
(
    id BIGINT NOT NULL,
    name VARCHAR(255),
    displayname VARCHAR(255),
    description TEXT,
    network_id BIGINT REFERENCES network (id),
    databaseserver_id BIGINT REFERENCES databaseserver (id),
    tags TEXT,
    environment_id TEXT,
    PRIMARY KEY (id)
);

CREATE SEQUENCE dbstack_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE UNIQUE INDEX IF NOT EXISTS dbstack_id_idx ON dbstack(id);
CREATE INDEX IF NOT EXISTS dbstack_name_idx ON dbstack(name);

CREATE TABLE dbstack_parameters
(
    dbstack_id BIGINT NOT NULL REFERENCES dbstack (id),
    key VARCHAR(255) NOT NULL,
    value text,
    PRIMARY KEY (dbstack_id, key)
);

-- //@UNDO
-- SQL to undo the change goes here.

DROP TABLE dbstack_parameters;

DROP INDEX IF EXISTS dbstack_name_idx;
DROP INDEX IF EXISTS dbstack_id_idx;

DROP SEQUENCE IF EXISTS dbstack_id_seq;

DROP TABLE dbstack;

DROP INDEX IF EXISTS databaseserver_name_idx;
DROP INDEX IF EXISTS databaseserver_id_idx;

DROP SEQUENCE IF EXISTS databaseserver_id_seq;

DROP TABLE databaseserver;

DROP TABLE securitygroup_securitygroupids;

DROP INDEX IF EXISTS securitygroup_name_idx;
DROP INDEX IF EXISTS securitygroup_id_idx;

DROP SEQUENCE IF EXISTS securitygroup_id_seq;

DROP TABLE securitygroup;

DROP INDEX IF EXISTS network_name_idx;
DROP INDEX IF EXISTS network_id_idx;

DROP SEQUENCE IF EXISTS network_id_seq;

DROP TABLE network;
