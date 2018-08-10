-- // BUG-107734 cleanup-ldap-configs-for-org
-- Migration SQL that makes the change goes here.

ALTER TABLE ldapconfig ALTER COLUMN owner DROP NOT NULL;
ALTER TABLE ldapconfig ALTER COLUMN account DROP NOT NULL;
ALTER TABLE ldapconfig DROP CONSTRAINT IF EXISTS uk_ldapconfig_account_name;

-- //@UNDO
-- SQL to undo the change goes here.

