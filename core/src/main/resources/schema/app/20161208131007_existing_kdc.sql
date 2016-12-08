-- // existing kdc
-- Migration SQL that makes the change goes here.


ALTER TABLE cluster ADD COLUMN kerberosurl VARCHAR(255);
ALTER TABLE cluster ADD COLUMN kerberosrealm VARCHAR(255);
ALTER TABLE cluster ADD COLUMN kerberosdomain VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster DROP COLUMN kerberosurl;
ALTER TABLE cluster DROP COLUMN kerberosrealm;
ALTER TABLE cluster DROP COLUMN kerberosdomain;


