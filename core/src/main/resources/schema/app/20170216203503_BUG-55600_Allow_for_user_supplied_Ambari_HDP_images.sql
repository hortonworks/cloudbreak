-- // BUG-55600 Allow for user supplied Ambari HDP images
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ADD COLUMN customContainerDefinition TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN customContainerDefinition;

