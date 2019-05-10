-- // CB-1210 create workspace table
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS workspace (
    id                  bigserial NOT NULL,
    name                character varying(255) NOT NULL,
    tenant_id           bigint NOT NULL,
    description         text,
    status              character varying(255) DEFAULT 'ACTIVE'::character varying,
    deletiontimestamp   bigint NOT NULL DEFAULT '-1'::integer,
    resourcecrn         text,
    CONSTRAINT fk_workspace_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT org_in_tenant_deletiondate_unique UNIQUE (name, deletiontimestamp, tenant_id),
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS workspace_id_idx ON workspace USING btree (id);

CREATE INDEX IF NOT EXISTS workspace_deletiontimestamp_idx ON workspace USING btree (deletiontimestamp);
CREATE INDEX IF NOT EXISTS workspace_name_idx ON workspace USING btree (name);
CREATE INDEX IF NOT EXISTS workspace_tenant_id_idx ON workspace USING btree (tenant_id);


-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS workspace_id_idx;
DROP INDEX IF EXISTS workspace_deletiontimestamp_idx;
DROP INDEX IF EXISTS workspace_name_idx;
DROP INDEX IF EXISTS workspace_tenant_id_idx;

DROP TABLE IF EXISTS workspace;
