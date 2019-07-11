-- // CB-2366 Add connection driver and jar to stack db server
-- Migration SQL that makes the change goes here.

ALTER TABLE databaseserver
    ADD COLUMN connectiondriver VARCHAR(255) DEFAULT 'org.postgresql.Driver',
    ADD COLUMN connectorjarurl VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE databaseserver
    DROP COLUMN connectiondriver,
    DROP COLUMN connectorjarurl;
