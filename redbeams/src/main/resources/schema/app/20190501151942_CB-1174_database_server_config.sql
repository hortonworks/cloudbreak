-- CB-1174 database server config

CREATE TABLE databaseserverconfig
(
   id                   BIGINT NOT NULL,
   workspace_id         INT8,
   name                 VARCHAR(255) NOT NULL DEFAULT 'name',
   description          TEXT,
   host                 VARCHAR(255),
   port                 INT4,
   databasevendor       VARCHAR(255) NOT NULL,
   connectiondriver     VARCHAR(255) DEFAULT 'org.postgresql.Driver',
   connectionusername   VARCHAR(255) NOT NULL,
   connectionpassword   VARCHAR(255) NOT NULL,
   creationdate         BIGINT,
   resourcestatus       VARCHAR(255) NOT NULL DEFAULT 'USER_MANAGED',
   connectorjarurl      VARCHAR(255),
   archived             BOOLEAN DEFAULT FALSE,
   deletionTimestamp    BIGINT DEFAULT -1,
   PRIMARY KEY (id)
);

CREATE SEQUENCE databaseserverconfig_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE UNIQUE INDEX IF NOT EXISTS databaseserverconfig_id_idx ON databaseserverconfig(id);
CREATE INDEX IF NOT EXISTS databaseserverconfig_name_idx ON databaseserverconfig(name);
CREATE INDEX IF NOT EXISTS databaseserverconfig_workspace_id_idx ON databaseserverconfig(workspace_id);
CREATE INDEX IF NOT EXISTS databaseserverconfig_workspace_id_name_idx ON databaseserverconfig(workspace_id, name);

--  ADD CONSTRAINT fk_rdsconfig_organization FOREIGN KEY (organization_id) REFERENCES organization(id);

-- FIXME
-- ADD CONSTRAINT uk_databaseserverconfig_deletiondate_workspace UNIQUE (name, deletionTimestamp, workspace_id);

-- //@UNDO

DROP INDEX IF EXISTS databaseserverconfig_workspace_id_name_idx;
DROP INDEX IF EXISTS databaseserverconfig_workspace_id_idx;
DROP INDEX IF EXISTS databaseserverconfig_name_idx;
DROP INDEX IF EXISTS databaseserverconfig_id_idx;

DROP TABLE IF EXISTS databaseserverconfig;

DROP SEQUENCE IF EXISTS databaseserverconfig_id_seq;
