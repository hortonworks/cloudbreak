-- // CLOUD-58085_persist_flow_variables
-- Migration SQL that makes the change goes here.

ALTER TABLE flowlog ADD COLUMN variables TEXT;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE flowlog DROP COLUMN variables;