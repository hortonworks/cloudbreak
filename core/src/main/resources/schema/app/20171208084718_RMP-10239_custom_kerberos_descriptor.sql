-- // RMP-10239_custom_kerberos_descriptor
-- Migration SQL that makes the change goes here.

ALTER TABLE kerberosconfig ADD COLUMN kerberosdescriptor TEXT;
ALTER TABLE kerberosconfig ADD COLUMN krb5conf TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE kerberosconfig DROP COLUMN kerberosdescriptor;
ALTER TABLE kerberosconfig DROP COLUMN krb5conf;