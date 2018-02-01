-- // RMP-10378_persist_kerberos_type
-- Migration SQL that makes the change goes here.

ALTER TABLE kerberosconfig ADD COLUMN type varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE kerberosconfig DROP COLUMN type;