-- // DPAAS-83 rename ambari admin group for ldap config
-- Migration SQL that makes the change goes here.

ALTER TABLE ldapconfig RENAME COLUMN ambariadmingroup to admingroup;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ldapconfig RENAME COLUMN admingroup to ambariadmingroup;