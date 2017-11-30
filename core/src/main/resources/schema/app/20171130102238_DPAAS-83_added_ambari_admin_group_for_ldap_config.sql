-- // DPAAS-83 added ambari admin group for ldap config
-- Migration SQL that makes the change goes here.

ALTER TABLE ldapconfig ADD COLUMN IF NOT EXISTS ambariadmingroup text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ldapconfig DROP COLUMN IF EXISTS ambariadmingroup;
