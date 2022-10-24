-- // CB-18629 - All flow logs should have non zero resource id
-- Migration SQL that makes the change goes here.
UPDATE flowlog f
SET resourceid =
    (
    SELECT MAX(resourceid)
    FROM flowlog
    WHERE f.flowid = flowid
    )
WHERE resourceid = 0;


-- //@UNDO
-- SQL to undo the change goes here.
-- Left blank, no point in rollback


