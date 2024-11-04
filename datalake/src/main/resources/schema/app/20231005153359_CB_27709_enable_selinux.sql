-- // CB-27705_add_selinux_policy_to_stack_table.sql
-- Migration SQL that makes the change goes here.

ALTER TABLE sdxcluster ADD COLUMN IF NOT EXISTS selinux varchar(255) DEFAULT 'PERMISSIVE';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE sdxcluster DROP COLUMN IF EXISTS selinux;
