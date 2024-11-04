-- // CB-27707 SeLinuxPolicy added to stack object
-- Migration SQL that makes the change goes here.

ALTER TABLE securityconfig ADD COLUMN IF NOT EXISTS selinux varchar(255) DEFAULT 'PERMISSIVE';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE securityconfig DROP COLUMN IF EXISTS selinux;
