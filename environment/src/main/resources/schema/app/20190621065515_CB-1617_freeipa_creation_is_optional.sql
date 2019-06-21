-- // CB-1617 freeipa creation is optional
-- Migration SQL that makes the change goes here.

ALTER TABLE environment ADD COLUMN IF NOT EXISTS statusReason text;

ALTER TABLE environment ADD COLUMN created BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM now()) * 1000);

UPDATE environment set status='CREATE_FAILED' WHERE status='CORRUPTED';

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment DROP COLUMN IF EXISTS statusReason;

ALTER TABLE environment DROP COLUMN IF EXISTS created;

UPDATE environment set status='CORRUPTED' WHERE status='CREATE_FAILED';