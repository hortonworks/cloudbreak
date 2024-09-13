-- // CB-27192
-- Migration SQL that makes the change goes here.

UPDATE stack SET architecture = 'X86_64' WHERE architecture IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.
