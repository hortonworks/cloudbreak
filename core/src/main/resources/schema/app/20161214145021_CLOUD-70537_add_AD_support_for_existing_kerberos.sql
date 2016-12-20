-- // CLOUD-70537 add AD support for existing kerberos
-- Migration SQL that makes the change goes here.

ALTER TABLE kerberosconfig ADD COLUMN kerberosldapurl VARCHAR(255);
ALTER TABLE kerberosconfig ADD COLUMN kerberoscontainerdn VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE kerberosconfig DROP COLUMN kerberosldapurl VARCHAR(255);
ALTER TABLE kerberosconfig DROP COLUMN kerberoscontainerdn VARCHAR(255);
