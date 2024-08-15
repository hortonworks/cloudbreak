-- // CB-26863 introduce userCrn into the structured events
-- Migration SQL that makes the change goes here.

UPDATE stack SET javaversion=8 WHERE javaversion is NULL;
ALTER TABLE stack ALTER COLUMN javaversion SET DEFAULT 8;

-- //@UNDO
-- SQL to undo the change goes here.
