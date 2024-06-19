-- // CB-26111 index on status table with id and datalakeid
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS index_datalake_id ON sdxstatus(datalake, id DESC);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS index_datalake_id;

