-- // CB-6380 Do not persist telemetry component for stack with template type
-- Migration SQL that makes the change goes here.

DELETE FROM component WHERE componenttype = 'TELEMETRY' AND stack_id IN (SELECT id FROM stack WHERE type = 'TEMPLATE')

-- //@UNDO
-- SQL to undo the change goes here.