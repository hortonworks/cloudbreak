-- // Create Changelog

-- Default DDL for changelog table that will keep
-- a record of the migrations that have been run.

-- You can modify this to suit your database before
-- running your first migration.

-- Be sure that ID and DESCRIPTION fields exist in
-- BigInteger and String compatible fields respectively.

ALTER TABLE stack
    ADD tenant VARCHAR(255);

CREATE UNIQUE INDEX tenant_uindex
    ON stack (tenant);

CREATE UNIQUE INDEX environment_tenant_uindex
    ON stack (name, environment, tenant);

CREATE UNIQUE INDEX stack_name_environment_tenant_uindex
    ON stack (name, environment, tenant);

-- //@UNDO

DROP INDEX tenant_uindex;

DROP INDEX environment_tenant_uindex;

DROP INDEX stack_name_environment_tenant_uindex;

ALTER TABLE stack DROP COLUMN tenant;