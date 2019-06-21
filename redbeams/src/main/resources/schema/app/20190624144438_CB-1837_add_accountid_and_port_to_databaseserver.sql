-- // CB-1837 Add account ID and port to database server
-- Migration SQL that makes the change goes here.

ALTER TABLE databaseserver
    ADD COLUMN accountid VARCHAR(255),
    ADD COLUMN port INT4;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE databaseserver
    DROP COLUMN accountid,
    DROP COLUMN port;
