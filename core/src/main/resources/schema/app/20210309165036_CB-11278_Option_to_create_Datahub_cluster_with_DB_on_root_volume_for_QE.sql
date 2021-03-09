-- // CB-11278 Option to create Datahub cluster with DB on root volume for QE
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ALTER COLUMN externaldatabasecreationtype TYPE varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.

-- // No undo SQL is provided as the new enum values can be longer than 10 characters
