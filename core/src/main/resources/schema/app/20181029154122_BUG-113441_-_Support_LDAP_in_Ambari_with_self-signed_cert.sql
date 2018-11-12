-- // BUG-113441 - Support LDAP in Ambari with self-signed cert
-- Migration SQL that makes the change goes here.

ALTER TABLE ldapconfig ADD COLUMN certificate TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE ldapconfig DROP COLUMN IF EXISTS certificate;


