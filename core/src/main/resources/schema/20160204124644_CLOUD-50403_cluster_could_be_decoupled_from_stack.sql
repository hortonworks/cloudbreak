-- // CLOUD-50403 cluster could be decoupled from stack
-- Migration SQL that makes the change goes here.

ALTER TABLE cluster ALTER COLUMN stack_id DROP NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE cluster ALTER COLUMN stack_id SET NOT NULL;

