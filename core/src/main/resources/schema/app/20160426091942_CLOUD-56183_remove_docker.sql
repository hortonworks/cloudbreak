-- // CLOUD-56183 remove docker
-- Migration SQL that makes the change goes here.

ALTER TABLE orchestrator ALTER COLUMN apiendpoint DROP NOT NULL

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE orchestrator ALTER COLUMN apiendpoint SET NOT NULL

