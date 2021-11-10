-- // CB-14678 add domain field to environment table
-- Migration SQL that makes the change goes here.
ALTER TABLE environment ADD COLUMN IF NOT EXISTS environment_domain varchar(255);


-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment DROP COLUMN IF EXISTS environment_domain;
