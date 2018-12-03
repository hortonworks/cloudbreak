-- // BUG-114198 add new indexes for faster stack responses
-- Migration SQL that makes the change goes here.


CREATE INDEX IF NOT EXISTS stackstatus_status_idx ON stackstatus USING btree (status);

CREATE INDEX IF NOT EXISTS stack_stackstatus_id_idx ON stack USING btree (stackstatus_id);


-- //@UNDO
-- SQL to undo the change goes here.

DROP INDEX IF EXISTS stackstatus_status_idx;

DROP INDEX IF EXISTS stack_stackstatus_id_idx;


