-- // CB-16132 create operationId index for operation
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS operation_operationid_idx
    ON operation (operationid);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS operation_operationid_idx;
