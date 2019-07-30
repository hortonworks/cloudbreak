-- // CB-2687 Remove connector JAR URL
-- Migration SQL that makes the change goes here.

-- Existing URL values are lost!

ALTER TABLE databaseserver
    DROP COLUMN connectorjarurl;

ALTER TABLE databaseserverconfig
    DROP COLUMN connectorjarurl;

ALTER TABLE databaseconfig
    DROP COLUMN connectorjarurl;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE databaseconfig
    ADD COLUMN connectorjarurl VARCHAR(255);

ALTER TABLE databaseserverconfig
    ADD COLUMN connectorjarurl VARCHAR(255);

ALTER TABLE databaseserver
    ADD COLUMN connectorjarurl VARCHAR(255);
