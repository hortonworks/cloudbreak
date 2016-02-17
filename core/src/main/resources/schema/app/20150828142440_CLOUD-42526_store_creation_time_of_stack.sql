-- // CLOUD-42526 store creation time of stack
-- Migration SQL that makes the change goes here.

ALTER TABLE stack ADD COLUMN created BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM now()) * 1000);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE stack DROP COLUMN created;