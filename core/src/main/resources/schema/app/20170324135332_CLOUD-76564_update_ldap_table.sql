-- // CLOUD-76564 update ldap table
-- Migration SQL that makes the change goes here.

ALTER TABLE ldapconfig ADD COLUMN protocol varchar(255);

UPDATE ldapconfig SET protocol = 'ldap';
UPDATE ldapconfig SET protocol = 'ldaps' WHERE serverssl = true;

ALTER TABLE ldapconfig ALTER COLUMN protocol SET NOT NULL;

ALTER TABLE ldapconfig DROP COLUMN serverssl;

-- //@UNDO
-- SQL to undo the change goes here.


ALTER TABLE ldapconfig ADD COLUMN serverssl boolean;

UPDATE ldapconfig SET serverssl = false;
UPDATE ldapconfig SET protocol = true WHERE protocol = 'ldaps';

ALTER TABLE ldapconfig ALTER COLUMN serverssl SET NOT NULL;

ALTER TABLE ldapconfig DROP COLUMN protocol;
