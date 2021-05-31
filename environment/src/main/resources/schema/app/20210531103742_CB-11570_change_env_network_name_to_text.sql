-- // CB-111570 Change env network name to text
-- Migration SQL that makes the change goes here.

ALTER TABLE environment_network ALTER COLUMN name TYPE text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_network ALTER COLUMN name TYPE varchar(255);
