-- // CB-1173 register existing databaseconfig
-- Migration SQL that makes the change goes here.

CREATE TABLE databaseconfig
(
   id                   BIGINT NOT NULL,
   crn                  VARCHAR(255) NOT NULL,
   name                 VARCHAR(255) NOT NULL DEFAULT 'name',
   description          TEXT,
   connectionurl        VARCHAR(255) NOT NULL,
   database_vendor      VARCHAR(255) NOT NULL,
   connectiondriver     VARCHAR(255) DEFAULT 'org.postgresql.Driver',
   connectionusername   VARCHAR(255) NOT NULL,
   connectionpassword   VARCHAR(255) NOT NULL,
   creationdate         BIGINT,
   resourcestatus       VARCHAR(255) NOT NULL DEFAULT 'USER_MANAGED',
   type                 VARCHAR(255) NOT NULL,
   connectorjarurl      VARCHAR(255),
   environment_id       VARCHAR(255) NOT NULL,
   archived             BOOLEAN DEFAULT FALSE,
   deletionTimestamp    BIGINT DEFAULT -1,
   PRIMARY KEY (id)
);

CREATE SEQUENCE databaseconfig_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

ALTER TABLE databaseconfig
    ADD CONSTRAINT uk_databaseconfig_deletiondate_environment UNIQUE (name, deletionTimestamp, environment_id);
CREATE UNIQUE INDEX IF NOT EXISTS databaseconfig_id_idx ON databaseconfig(id);
CREATE UNIQUE INDEX IF NOT EXISTS databaseconfig_crn_idx ON databaseconfig(crn);
CREATE INDEX IF NOT EXISTS databaseconfig_name_idx ON databaseconfig(name);

-- //@UNDO

DROP INDEX IF EXISTS databaseconfig_id_idx;
DROP INDEX IF EXISTS databaseconfig_name_idx;
DROP INDEX IF EXISTS databaseconfig_crn_idx;

DROP TABLE IF EXISTS databaseconfig;

DROP SEQUENCE IF EXISTS databaseconfig_id_seq;

-- //@UNDO
-- SQL to undo the change goes here.


