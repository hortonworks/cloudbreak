-- // RMP-10343_Separate_kdc_server_and_admin_host
-- Migration SQL that makes the change goes here.


ALTER TABLE kerberosconfig ADD COLUMN kdcadminurl VARCHAR(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE kerberosconfig DROP COLUMN kdcadminurl;