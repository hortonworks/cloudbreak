-- // RMP-12464 Add domain and nameservers column to kerberosconfig
-- Migration SQL that makes the change goes here.

ALTER TABLE kerberosconfig ADD domain varchar(255) NULL;
ALTER TABLE kerberosconfig ADD nameservers varchar(255) NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE kerberosconfig DROP COLUMN IF EXISTS domain;
ALTER TABLE kerberosconfig DROP COLUMN IF EXISTS nameservers;

