-- // introduce users group in ldap resource
-- Migration SQL that makes the change goes here.

ALTER TABLE ldapconfig ADD COLUMN usergroup TEXT;
UPDATE ldapconfig SET usergroup='ipausers';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ldapconfig DROP COLUMN IF EXISTS usergroup;
