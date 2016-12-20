-- // CLOUD-70537 remove domain from kerberos config
-- Migration SQL that makes the change goes here.

ALTER TABLE kerberosconfig DROP COLUMN kerberosdomain;
ALTER TABLE kerberosconfig ADD COLUMN kerberostcpallowed boolean DEFAULT false;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE kerberosconfig ADD COLUMN kerberosdomain VARCHAR(255);
ALTER TABLE kerberosconfig DROP COLUMN kerberostcpallowed;
