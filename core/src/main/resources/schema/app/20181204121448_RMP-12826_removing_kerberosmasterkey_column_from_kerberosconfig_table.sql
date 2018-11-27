-- // RMP-12826 removing kerberosmasterkey column from kerberosconfig table
-- Migration SQL that makes the change goes here.
ALTER TABLE kerberosconfig
DROP COLUMN IF EXISTS kerberosmasterkey;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE kerberosconfig
ADD COLUMN IF NOT EXISTS kerberosmasterkey VARCHAR(255);
