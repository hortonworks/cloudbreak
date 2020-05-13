-- // CB-6886 DB Stack status should not be null
-- Migration SQL that makes the change goes here.
UPDATE dbstackstatus SET status = 'UNKNOWN' WHERE status = NULL;
ALTER TABLE dbstackstatus ALTER COLUMN status SET NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE dbstackstatus ALTER COLUMN status DROP NOT NULL;
UPDATE dbstackstatus SET status = NULL WHERE status = 'UNKNOWN';
