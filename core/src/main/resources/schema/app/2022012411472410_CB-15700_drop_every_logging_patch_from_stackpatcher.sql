-- // CB-15700 Drop every metering azure metadata fix patches
-- Migration SQL that makes the change goes here.
DELETE FROM "stackpatch" WHERE type = 'LOGGING_AGENT_AUTO_RESTART';
-- //@UNDO
-- SQL to undo the change goes here.