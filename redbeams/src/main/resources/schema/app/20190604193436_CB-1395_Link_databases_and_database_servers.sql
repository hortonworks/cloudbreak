-- // CB-1395 Link databases and database servers
-- Migration SQL that makes the change goes here.

ALTER TABLE databaseconfig ADD COLUMN server_id bigint REFERENCES databaseserverconfig(id);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE databaseconfig DROP COLUMN server_id;
