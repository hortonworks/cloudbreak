-- // CB-13176 Gather AWS load balancer configs
-- Migration SQL that makes the change goes here.

ALTER TABLE loadbalancer ADD COLUMN IF NOT EXISTS providerconfig TEXT NULL;
ALTER TABLE targetgroup ADD COLUMN IF NOT EXISTS providerconfig TEXT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE loadbalancer DROP COLUMN IF EXISTS providerconfig;
ALTER TABLE targetgroup DROP COLUMN IF EXISTS providerconfig;
