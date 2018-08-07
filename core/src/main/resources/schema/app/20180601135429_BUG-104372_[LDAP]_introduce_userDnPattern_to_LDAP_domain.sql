-- // BUG-104372 [LDAP] introduce userDnPattern to LDAP domain
-- Migration SQL that makes the change goes here.

ALTER TABLE ldapconfig ADD COLUMN IF NOT EXISTS userDnPattern VARCHAR(255);
UPDATE ldapconfig set userDnPattern='cn={0},' || userSearchBase;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ldapconfig DROP COLUMN IF EXISTS userDnPattern;
