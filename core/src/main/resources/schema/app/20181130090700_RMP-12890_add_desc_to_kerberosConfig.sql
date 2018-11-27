-- // RMP-12890 add description to kerberosconfig KerberosConfig
-- Migration SQL that makes the change goes here.

ALTER TABLE kerberosconfig ADD COLUMN IF NOT EXISTS description TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE kerberosconfig DROP COLUMN IF EXISTS description;

