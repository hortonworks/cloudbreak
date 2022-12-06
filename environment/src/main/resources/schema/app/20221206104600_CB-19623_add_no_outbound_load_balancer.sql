-- // CB-19623 - Add option to disable  outbound load balancer creation
-- Migration SQL that makes the change goes here.
ALTER TABLE environment_parameters ADD COLUMN IF NOT EXISTS no_outbound_load_balancer bool NULL DEFAULT false;

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment_parameters DROP COLUMN IF EXISTS no_outbound_load_balancer;

