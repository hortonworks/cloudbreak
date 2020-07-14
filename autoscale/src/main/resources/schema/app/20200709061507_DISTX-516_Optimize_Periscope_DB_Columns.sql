-- // DISTX-516 OptimizePeriscopeDBColumns
-- Migration SQL that makes the change goes here.

-- Legacy periscope cluster data might have duplicate clusterpertains and uninitialized fields
delete from failed_node;
delete from history_properties;
delete from history;
delete from prometheusalert;
delete from loadalert;
delete from timealert;
delete from metricalert;
delete from scalingpolicy;
delete from securityconfig;
delete from cluster;
delete from cluster_manager;
delete from clusterpertain;


ALTER TABLE cluster ALTER COLUMN cb_stack_name SET NOT NULL;

ALTER TABLE cluster ALTER COLUMN cb_stack_type SET NOT NULL;

ALTER TABLE cluster ALTER COLUMN cloud_platform SET NOT NULL;

ALTER TABLE clusterpertain ALTER COLUMN  usercrn SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_cluster_cb_stack_name ON cluster (cb_stack_name);

CREATE INDEX IF NOT EXISTS idx_clusterpertain_tenant ON clusterpertain (tenant);

CREATE INDEX IF NOT EXISTS idx_clusterpertain_usercrn ON clusterpertain (usercrn);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_clusterpertain_usercrn;

DROP INDEX IF EXISTS idx_clusterpertain_tenant;

DROP INDEX IF EXISTS idx_cluster_cb_stack_name;

ALTER TABLE clusterpertain ALTER COLUMN usercrn DROP NOT NULL;

ALTER TABLE cluster ALTER COLUMN cloud_platform DROP NOT NULL;

ALTER TABLE cluster ALTER COLUMN cb_stack_type DROP NOT NULL;

ALTER TABLE cluster ALTER COLUMN cb_stack_name DROP NOT NULL;