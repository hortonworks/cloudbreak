-- // CB-6385 add version to stack
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD IF NOT EXISTS version BIGINT;
ALTER TABLE stack ALTER COLUMN version SET DEFAULT 0;
UPDATE stack SET version = 0 WHERE version IS NULL AND terminated = -1;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP IF EXISTS version;

