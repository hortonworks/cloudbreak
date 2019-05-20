-- // Create Changelog

-- Default DDL for changelog table that will keep
-- a record of the migrations that have been run.

-- You can modify this to suit your database before
-- running your first migration.

-- Be sure that ID and DESCRIPTION fields exist in
-- BigInteger and String compatible fields respectively.

ALTER TABLE stack
    ADD tenant VARCHAR(255);

CREATE INDEX tenant_uindex
    ON stack (tenant);

CREATE INDEX environment_tenant_uindex
    ON stack (environment, tenant);

-- //@UNDO

DROP INDEX tenant_uindex;

DROP INDEX environment_tenant_uindex;

ALTER TABLE stack DROP COLUMN tenant;