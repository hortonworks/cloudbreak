-- // CB-15466 cleanup operation indexes
-- Migration SQL that makes the change goes here.

ALTER TABLE operation DROP CONSTRAINT IF EXISTS syncoperation_operationid_key;

DROP INDEX IF EXISTS syncoperation_operationid_key;

DROP INDEX IF EXISTS syncoperation_id_idx;

DROP INDEX IF EXISTS syncoperation_operationid_idx;

DROP INDEX IF EXISTS syncoperation_accountid_endtime_idx;

CREATE INDEX IF NOT EXISTS operation_start_end_type_idx ON operation (starttime, endtime, operationtype);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS operation_start_end_type_idx;


