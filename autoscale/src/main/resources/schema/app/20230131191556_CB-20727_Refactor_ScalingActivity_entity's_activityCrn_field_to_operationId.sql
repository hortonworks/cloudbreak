-- // CB-20727 Refactor ScalingActivity entity's activityCrn field to operationId
-- Migration SQL that makes the change goes here.

ALTER TABLE scaling_activity ADD COLUMN IF NOT EXISTS operation_id VARCHAR(255);

create unique index if not exists idx_operation_id on scaling_activity(operation_id);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS idx_operation_id;

ALTER TABLE scaling_activity DROP COLUMN IF EXISTS operation_id;