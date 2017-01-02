-- // CLOUD-71449 added domain to ldapconfig
-- Migration SQL that makes the change goes here.

ALTER TABLE ldapconfig ADD COLUMN domain varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ldapconfig DROP COLUMN domain;


