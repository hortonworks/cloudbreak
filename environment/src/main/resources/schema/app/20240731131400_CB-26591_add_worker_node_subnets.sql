-- // CB-26591 - Worker node subnets for compute cluster
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS compute_worker_node_subnets text;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS compute_worker_node_subnets;