-- // BUG-112199 StackView: adding db indexes to cut retrieval time
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS instancegroup_stack_id ON instancegroup USING btree (stack_id);
CREATE INDEX IF NOT EXISTS instancemetadata_instancegroup_id ON instancemetadata USING btree (instancegroup_id);
CREATE INDEX IF NOT EXISTS hostgroup_cluster_id ON hostgroup USING btree (cluster_id);
CREATE INDEX IF NOT EXISTS hostmetadata_hostgroup_id ON hostmetadata USING btree (hostgroup_id);
CREATE INDEX IF NOT EXISTS cluster_stack_id ON cluster USING btree (stack_id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS instancegroup_stack_id;
DROP INDEX IF EXISTS instancemetadata_instancegroup_id;
DROP INDEX IF EXISTS hostgroup_cluster_id;
DROP INDEX IF EXISTS hostmetadata_hostgroup_id;
DROP INDEX IF EXISTS cluster_stack_id;


