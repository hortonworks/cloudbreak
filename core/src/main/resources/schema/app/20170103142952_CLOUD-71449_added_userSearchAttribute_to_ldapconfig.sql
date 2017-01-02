-- // CLOUD-71449 added userSearchAttribute to ldapconfig
-- Migration SQL that makes the change goes here.

ALTER TABLE ldapconfig ADD COLUMN usersearchattribute varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ldapconfig DROP COLUMN usersearchattribute;

