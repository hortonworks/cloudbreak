-- // CB-25865 -- Remove deleted compute_load_balancer_autohirzed_ip_ranges parameter
-- Migration SQL that makes the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS compute_load_balancer_authorized_ip_ranges;


-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS compute_load_balancer_authorized_ip_ranges varchar(255);

