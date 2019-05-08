-- // Create Changelog

-- Default DDL for changelog table that will keep
-- a record of the migrations that have been run.

-- You can modify this to suit your database before
-- running your first migration.

-- Be sure that ID and DESCRIPTION fields exist in
-- BigInteger and String compatible fields respectively.

ALTER TABLE stack
    ADD environment VARCHAR(255);

CREATE UNIQUE INDEX stack_name_environment_uindex
    ON stack (name, environment);

-- //@UNDO

DROP INDEX stack_name_environment_uindex;

ALTER TABLE stack DROP COLUMN environment;