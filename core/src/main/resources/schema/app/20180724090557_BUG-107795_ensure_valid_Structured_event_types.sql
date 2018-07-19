-- // BUG-107795 ensure valid Structured event types
-- Migration SQL that makes the change goes here.

DELETE FROM structuredevent WHERE eventtype NOT IN ('NOTIFICATION', 'FLOW', 'REST');


-- //@UNDO
-- SQL to undo the change goes here.


