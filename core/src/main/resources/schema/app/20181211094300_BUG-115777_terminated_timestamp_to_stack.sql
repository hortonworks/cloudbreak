-- // BUG-115777 add terminated timestamp to stack
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN terminated BIGINT;
UPDATE stack SET terminated = (EXTRACT(EPOCH FROM now()) * 1000) WHERE stackstatus_id = (SELECT id FROM stackstatus WHERE status LIKE 'DELETE_COMPLETED' AND id = stackstatus_id);
CREATE INDEX stack_workspaceid_terminated_idx ON stack (workspace_id, terminated);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX stack_workspaceid_terminated_idx;

ALTER TABLE stack DROP COLUMN terminated;