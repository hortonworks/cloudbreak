-- // CB-20153 - Move noOutboundLoadBalancer to network.
-- Migration SQL that makes the change goes here.
ALTER TABLE environment_parameters DROP COLUMN IF EXISTS no_outbound_load_balancer;
ALTER TABLE environment_network ADD COLUMN IF NOT EXISTS nooutboundloadbalancer bool NULL DEFAULT false;

-- //@UNDO
-- SQL to undo the change goes here.
ALTER TABLE environment_network DROP COLUMN IF EXISTS nooutboundloadbalancer;
ALTER TABLE environment_parameters ADD COLUMN IF NOT EXISTS no_outbound_load_balancer bool NULL DEFAULT false;

