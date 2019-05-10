-- // CB-1210 create tenant table
-- Migration SQL that makes the change goes here.

CREATE TABLE IF NOT EXISTS tenant (
    id                  bigserial NOT NULL,
    name                character varying(255) NOT NULL,
    description         text,
    CONSTRAINT tenant_name_is_unique UNIQUE (name),
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS tenant_id_idx ON tenant USING btree (id);
CREATE UNIQUE INDEX IF NOT EXISTS tenant_name_idx ON tenant USING btree (name);


-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS tenant_id_idx;
DROP INDEX IF EXISTS tenant_name_idx;

DROP TABLE IF EXISTS tenant;