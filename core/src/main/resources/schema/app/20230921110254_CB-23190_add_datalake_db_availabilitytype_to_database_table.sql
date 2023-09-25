-- // CB-23190 add datalake_db_availabilitytype to database table
-- Migration SQL that makes the change goes here.

ALTER TABLE database ADD IF NOT EXISTS datalake_db_availabilitytype VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE database DROP COLUMN IF EXISTS datalake_db_availabilitytype;
