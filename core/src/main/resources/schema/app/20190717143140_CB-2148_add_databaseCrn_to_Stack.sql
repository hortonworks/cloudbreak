-- // CB-2148 Add databaseServerCrn to stackrequest
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN IF NOT EXISTS databaseServerCrn varchar(512);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN IF EXISTS databaseServerCrn;