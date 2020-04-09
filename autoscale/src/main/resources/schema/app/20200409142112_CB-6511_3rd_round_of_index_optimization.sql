-- // CB-6511 3rd round of index optimization
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS idx_cluster_cb_stack_id on cluster(cb_stack_id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_cluster_cb_stack_id;

