-- // CB-25865 -- Remove deleted compute_load_balancer_autohirzed_ip_ranges parameter
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS selinux varchar(255) DEFAULT 'PERMISSIVE';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS selinux;

