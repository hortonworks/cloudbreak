-- // CB-12224 new index for detailed stack status
-- Migration SQL that makes the change goes here.

CREATE INDEX IF NOT EXISTS stackstatus_detailedstackstatus_idx ON stackstatus USING btree (detailedstackstatus);

-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS stackstatus_detailedstackstatus_idx;
