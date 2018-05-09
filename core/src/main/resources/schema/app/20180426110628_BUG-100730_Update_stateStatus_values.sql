-- // BUG-100730 Update stateStatus values
-- Migration SQL that makes the change goes here.
UPDATE flowlog SET statestatus = 'SUCCESSFUL';
UPDATE flowlog SET statestatus = 'PENDING' WHERE (flowid, id) IN (
    SELECT DISTINCT ON (flowid) flowid, id FROM flowlog WHERE finalized = false ORDER BY flowid, created DESC
);

-- //@UNDO
-- SQL to undo the change goes here.
UPDATE flowlog SET statestatus = NULL;

